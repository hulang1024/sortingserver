package sorting.api.packageitem;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import sorting.api.common.Page;
import sorting.api.common.PageParams;
import sorting.api.common.PageUtils;
import sorting.api.common.Result;
import sorting.api.item.Item;
import sorting.api.item.ItemRepo;
import sorting.api.item.QItem;
import sorting.api.packages.*;
import sorting.api.packages.Package;
import sorting.api.scheme.Scheme;
import sorting.api.scheme.SchemeRepo;
import sorting.api.user.QUser;
import sorting.api.user.SessionUserUtils;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/package_item_op")
public class PackageItemOpController {
    @Autowired
    private PackageItemOpRepo packageItemOpRepo;
    @Autowired
    private PackageItemRelRepo packageItemRelRepo;
    @Autowired
    private PackageRepo packageRepo;
    @Autowired
    private ItemRepo itemRepo;
    @Autowired
    private SchemeRepo schemeRepo;
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/page")
    public Page<PackageItemOpInfo> queryPage(@RequestParam Map<String, String> params, PageParams pageParams) {
        QPackageItemOp qPackageItemOp = QPackageItemOp.packageItemOp;
        QUser qUser = QUser.user;
        JPAQuery<?> query = new JPAQuery<>(entityManager)
            .select(Projections.bean(
                PackageItemOpInfo.class,
                qPackageItemOp.packageCode, qPackageItemOp.itemCode, qPackageItemOp.opType, qPackageItemOp.opTime,
                qUser.name.as("operatorName"), qUser.phone.as("operatorPhone")))
            .from(qPackageItemOp, qUser)
            .where(qPackageItemOp.operator.eq(qUser.id));

        if (!StringUtils.equals(params.get("fromAll"), "1")) {
            query.where(qPackageItemOp.operator.eq(SessionUserUtils.getUser().getId()));
        }
        if (StringUtils.isNotEmpty(params.get("opType"))) {
            query.where(qPackageItemOp.opType.eq(Integer.parseInt(params.get("opType"))));
        }
        if (StringUtils.isNotEmpty(params.get("packageCode"))) {
            query.where(qPackageItemOp.packageCode.like(params.get("packageCode") + "%"));
        }
        query.orderBy(qPackageItemOp.opTime.desc());
        return PageUtils.fetchPage(query, pageParams);
    }

    @PostMapping("/add_item")
    @Transactional
    public Result addItem(String packageCode, String itemCode, Long schemeId) {
        Optional<Package> pkgOpt = packageRepo.findById(packageCode);
        if (!pkgOpt.isPresent()) {
            return Result.fail(1).message("未查询到集包");
        }
        Optional<Scheme> schemeOpt = schemeRepo.findById(schemeId);
        if (!schemeOpt.isPresent()) {
            return Result.fail(2).message("未知模式");
        }
        if (!itemCode.matches(schemeOpt.get().getItemCodePattern())) {
            return Result.fail(3).message("快件编号有误");
        }
        Optional<Item> itemOpt = itemRepo.findById(itemCode);
        if (!itemOpt.isPresent()) {
            return Result.fail(4).message("未查询到快件");
        }
        Optional<PackageItemRel> relOpt = packageItemRelRepo.findByItemCode(itemCode);
        if (relOpt.isPresent()) {
            if (relOpt.get().getPackageCode().equals(packageCode)) {
                return Result.fail(5).message("快件早已加到集包");
            } else {
                return Result.fail(5).message("快件早已加到其它集包");
            }
        }

        Item item = itemOpt.get();

        if (!pkgOpt.get().getDestCode().equals(item.getDestCode())) {
            return Result.fail(6).message("快件和集包的目的地编号不相同");
        }

        item.setPackTime(new Date());
        itemRepo.save(item);

        PackageItemRel rel = new PackageItemRel();
        rel.setPackageCode(packageCode);
        rel.setItemCode(itemCode);
        rel.setOperator(SessionUserUtils.getUser().getId());
        rel.setCreateAt(new Date());
        rel = packageItemRelRepo.save(rel);
        if (rel == null) {
            return Result.fail();
        }

        return addOpRecord(PackageItemOp.builder()
            .packageCode(packageCode)
            .itemCode(itemCode)
            .opType(1)
            .build());
    }

    @PostMapping("/delete_item")
    @Transactional
    public Result deleteItem(String packageCode, String itemCode) {
        if (!packageRepo.existsById(packageCode)) {
            return Result.fail(1).message("未查询到集包");
        }

        Optional<Item> itemOpt = itemRepo.findById(itemCode);
        if (!itemOpt.isPresent()) {
            return Result.fail(4).message("未查询到快件");
        }
        if (itemOpt.get().getPackTime() == null
            || !packageItemRelRepo.existsByPackageCodeAndItemCode(packageCode, itemCode)) {
            return Result.fail(5).message("集包未加快件");
        }

        packageItemRelRepo.deleteByPackageCodeAndItemCode(packageCode, itemCode);

        itemOpt.get().setPackTime(null);
        itemRepo.save(itemOpt.get());

        return addOpRecord(PackageItemOp.builder()
            .packageCode(packageCode)
            .itemCode(itemCode)
            .opType(2)
            .build());
    }

    @PostMapping("/batch")
    @Transactional
    public Result batchAdd(@RequestBody List<PackageItemRel> relations, Long schemeId) {
        Optional<Scheme> schemeOpt = schemeRepo.findById(schemeId);
        if (!schemeOpt.isPresent()) {
            return Result.fail(2).message("未知模式");
        }

        final Result result = Result.ok(new HashMap<Integer, List<String>>());

        Function<List<PackageItemRel>, List<String>> puckItemCodeFunc = rs ->
            rs.stream().map(PackageItemRel::getItemCode).collect(Collectors.toList());
        BiFunction<Integer, List<String>, List<String>> putStatus = (status, itemCodes) -> {
            if (!itemCodes.isEmpty()) {
                ((Map<Integer, List<String>>)result.getData()).put(status, itemCodes);
            }
            return itemCodes;
        };

        List<String> itemCodes = puckItemCodeFunc.apply(relations);

        // 过滤出格式错误的快件编号
        List<String> wrongCodes = itemCodes.stream()
            .filter(code -> !code.matches(schemeOpt.get().getItemCodePattern()))
            .collect(Collectors.toList());
        putStatus.apply(2, wrongCodes);
        itemCodes.removeAll(wrongCodes); // 只保留格式格式正确的快件编号
        if (itemCodes.isEmpty()) {
            return result;
        }

        // 查询存在快件库中的快件
        Map<String, Item> itemMap = new HashMap<>();
        QItem qItem = QItem.item;
        new JPAQuery<>(entityManager)
            .select(Projections.bean(Item.class, qItem.code, qItem.destCode)).from(qItem)
            .where(qItem.code.in(itemCodes))
            .fetchResults().getResults()
            .forEach(item -> {
                itemMap.put(item.getCode(), item);
            });

        // 查询不存在快件库中的快件编号
        List<String> notExistsCodes = itemCodes.stream()
            .filter(code -> !itemMap.containsKey(code)).collect(Collectors.toList());
        putStatus.apply(3, notExistsCodes);
        itemCodes.removeAll(notExistsCodes); // 只保留存在快件库中的快件编号
        if (itemCodes.isEmpty()) {
            return result;
        }

        // 查询已存在集包快件关系库中的快件编号（已加到其它集包的）
        QPackageItemRel qPackageItemRel = QPackageItemRel.packageItemRel;
        List<String> existsRelCodes = new JPAQuery<>(entityManager)
            .select(qPackageItemRel.itemCode).from(qPackageItemRel)
            .where(qPackageItemRel.itemCode.in(itemCodes))
            .fetchResults().getResults();
        putStatus.apply(4, existsRelCodes);
        itemCodes.removeAll(existsRelCodes); // 只保留不存在关联关系的快件编号
        if (itemCodes.isEmpty()) {
            return result;
        }

        // 只保留基本验证成功的关系
        relations.removeIf(rel -> !itemMap.containsKey(rel.getItemCode()));

        // 查询集包
        QPackage qPackage = QPackage.package$;
        Map<String, Package> packageMap = new HashMap<>();
        new JPAQuery<>(entityManager)
            .select(Projections.bean(Package.class, qPackage.code, qPackage.destCode)).from(qPackage)
            .where(qPackage.code.in(
                relations.stream().map(PackageItemRel::getPackageCode).collect(Collectors.toSet())))
            .fetchResults().getResults()
            .forEach(pkg -> {
                packageMap.put(pkg.getCode(), pkg);
            });
        relations.removeIf(rel -> !packageMap.containsKey(rel.getPackageCode()));

        // 查询目的地不同的快件编号
        List<String> destCodeDiffItemCodes = new ArrayList<>();
        for (PackageItemRel rel : relations) {
            if (!itemMap.get(rel.getItemCode()).getDestCode()
                .equals(packageMap.get(rel.getPackageCode()).getDestCode())) {
                destCodeDiffItemCodes.add(rel.getItemCode());
                itemCodes.remove(rel.getItemCode());
                itemMap.remove(rel.getItemCode());
            }
        }
        putStatus.apply(5, destCodeDiffItemCodes);
        relations.removeIf(rel -> !itemMap.containsKey(rel.getItemCode()));
        if (relations.isEmpty()) {
            return result;
        }

        relations = (List<PackageItemRel>) packageItemRelRepo.saveAll(relations);
        List<PackageItemOp> opRecords = relations.stream().map(rel -> PackageItemOp.builder()
            .packageCode(rel.getPackageCode())
            .itemCode(rel.getItemCode())
            .opType(1)
            .build()
        ).collect(Collectors.toList());
        packageItemOpRepo.saveAll(opRecords);
        putStatus.apply(0, puckItemCodeFunc.apply(relations));
        return result;
    }

    public Result addOpRecord(PackageItemOp op) {
        op.setOperator(SessionUserUtils.getUser().getId());
        op.setOpTime(new Date());
        op = packageItemOpRepo.save(op);
        return Result.from(op != null);
    }
}
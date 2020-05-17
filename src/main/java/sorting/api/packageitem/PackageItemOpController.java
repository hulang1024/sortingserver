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
import sorting.api.packages.Package;
import sorting.api.packages.PackageRepo;
import sorting.api.packages.QPackage;
import sorting.api.scheme.Scheme;
import sorting.api.scheme.SchemeRepo;
import sorting.api.user.QUser;
import sorting.api.user.SessionUserUtils;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.function.BiFunction;
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
    public Result batch(@RequestBody List<PackageItemOp> ops, Long schemeId) {
        // 声明返回结果
        final Result result = Result.ok(new HashMap<Integer, List<Long>>());
        BiFunction<Integer, List<Long>, List<Long>> putStatus = (status, opIds) -> {
            if (!opIds.isEmpty()) {
                ((Map<Integer, List<Long>>)result.getData()).put(status, opIds);
            }
            return opIds;
        };

        // 定义一个函数：从操作列表中拿出快件编号组成新的列表
        Function<List<PackageItemOp>, List<String>> puckItemCodeFunc = rs ->
            rs.stream().map(PackageItemOp::getItemCode).collect(Collectors.toList());
        Function<List<PackageItemOp>, List<Long>> puckIdFunc = rs ->
            rs.stream().map(PackageItemOp::getId).collect(Collectors.toList());


        // 下面需要用到模式，先判断是否传递了模式
        Optional<Scheme> schemeOpt = schemeRepo.findById(schemeId);
        if (!schemeOpt.isPresent()) {
            return Result.fail(2).message("未知模式");
        }
        // 过滤出格式错误的快件编号
        List<PackageItemOp> wrongItemCodeOps = ops.stream()
            .filter(op -> !op.getItemCode().matches(schemeOpt.get().getItemCodePattern()))
            .collect(Collectors.toList());
        putStatus.apply(2, puckIdFunc.apply(wrongItemCodeOps));
        // 只保留快件编号格式正确的
        ops.removeAll(wrongItemCodeOps);
        if (ops.isEmpty()) {
            return result;
        }

        // 查询存在快件库中的快件
        Map<String, Item> itemMap = new HashMap<>();
        QItem qItem = QItem.item;
        new JPAQuery<>(entityManager)
            .select(qItem).from(qItem)
            .where(qItem.code.in(puckItemCodeFunc.apply(ops)))
            .fetchResults().getResults()
            .forEach(item -> {
                itemMap.put(item.getCode(), item);
            });

        // 过滤出不存在快件库中的快件编号
        List<PackageItemOp> notExistsItemOps = ops.stream()
            .filter(op -> !itemMap.containsKey(op.getItemCode()))
            .collect(Collectors.toList());
        putStatus.apply(3, puckIdFunc.apply(notExistsItemOps));
        // 只保留快件存在快件库中的
        ops.removeAll(notExistsItemOps);
        if (ops.isEmpty()) {
            return result;
        }

        // 查询集包
        QPackage qPackage = QPackage.package$;
        Map<String, Package> packageMap = new HashMap<>();
        new JPAQuery<>(entityManager)
            .select(Projections.bean(Package.class, qPackage.code, qPackage.destCode)).from(qPackage)
            .where(qPackage.code.in(ops.stream()
                .map(PackageItemOp::getPackageCode)
                .collect(Collectors.toSet())))
            .fetchResults().getResults()
            .forEach(pkg -> {
                packageMap.put(pkg.getCode(), pkg);
            });
        // 过滤出不存在集包库中的集包的所分配快件的编号
        List<PackageItemOp> notExitsPackageOps = ops.stream()
            .filter(op -> !packageMap.containsKey(op.getPackageCode()))
            .collect(Collectors.toList());
        putStatus.apply(4, puckIdFunc.apply(notExitsPackageOps));
        // 只保留存在集包库的集包的操作
        ops.removeAll(notExitsPackageOps);

        // 查询已存在的集包快件关系
        Map<String, PackageItemRel> itemCodeRelationMap = new HashMap<>();
        QPackageItemRel qPackageItemRel = QPackageItemRel.packageItemRel;
        new JPAQuery<>(entityManager)
            .select(qPackageItemRel).from(qPackageItemRel)
            .where(qPackageItemRel.itemCode.in(puckItemCodeFunc.apply(ops)))
            .fetchResults().getResults()
            .forEach(rel -> {
                itemCodeRelationMap.put(rel.getItemCode(), rel);
            });

        // 分类操作
        List<PackageItemOp> addOps = new ArrayList<>();
        List<PackageItemOp> deleteOps = new ArrayList<>();
        ops.forEach(op -> {
            switch (op.getOpType()) {
                case 1: addOps.add(op); break;
                case 2: deleteOps.add(op); break;
            }
        });

        // 对于加件操作，判断目的地不同的分配操作
        if (!addOps.isEmpty()) {
            List<PackageItemOp> destCodeDiffItemCodeOps = addOps.stream()
                .filter(op -> !itemMap.get(op.getItemCode()).getDestCode()
                    .equals(packageMap.get(op.getPackageCode()).getDestCode()))
                .collect(Collectors.toList());
            putStatus.apply(5, puckIdFunc.apply(destCodeDiffItemCodeOps));
            addOps.removeAll(destCodeDiffItemCodeOps);
        }

        // 前置条件，已做这些验证：
        //   集包和快件都存在。
        //   对于加件，集包和快件目的地相同。
        // 将继续做这些处理：
        //   对于加件，快件之前未增加到任何集包。
        //   对于减件，快件之前已增加到任何集包。
        // 不能这样简单的处理：
        //   对加件操作，验证集包快件关联不存在；
        //   对减件操作，验证集包快件关联已存在；
        //   然后根据加件和减件操作分别转换为数据库集包快件关联的增加和删除。
        // 因为在对于同个集包快件关联的两个互逆操作时，有操作状态设置和数据库集包快件关联增加和删除的执行顺序的问题。
        // 例如，有操作记录：在8:00时集包pA加件iA；在9:00时集包pA减件iA。
        // 应该先增加8点的数据库集包快件关联，然后再删除9点的数据库集包快件关联。
        // 但是接下来我们只会用到JPA的saveAll和deleteAll方法，被限制按类别执行，而不是根据单个加件/减件操作的时间戳执行。
        // 为了解决上述问题，我们需要更复杂的分情况处理：
        //   先加件后减件：
        //     如果数据库已存在集包快件关联，那么加件操作状态设置为"快件早已加到其它集包"，然后删除一个数据库集包快件关联。
        //     如果数据库不存在集包快件关联，那么加件操作状态设置为"成功"，减件操作状态设置为"成功"。
        //   先减件后加件：
        //     如果数据库已存在集包快件关联，那么减件操作状态设置为"成功"，加件操作状态设置为"成功"。
        //     如果数据库不存在集包快件关联，那么减件操作状态设置为"不存在快件"，然后增加一个数据库集包快件关联。
        //   上述设置为"成功"的操作不会去执行数据库，其它根据数据库执行结果来设置"成功"与否。
        List<PackageItemOp> successOps = new ArrayList<>();
        List<PackageItemOp> existsRelationOps = new ArrayList<>();
        List<PackageItemOp> notExistsRelationOps = new ArrayList<>();
        if (!(deleteOps.isEmpty() || addOps.isEmpty())) {
            deleteOps.forEach(delOp -> {
                for (PackageItemOp addOp : addOps) {
                    if (delOp.getPackageCode().equals(addOp.getPackageCode())
                        && delOp.getItemCode().equals(addOp.getItemCode())) {
                        // 先加件后减件
                        if (delOp.getOpTime().after(addOp.getOpTime())) {
                            if (itemCodeRelationMap.containsKey(addOp.getItemCode())) {
                                existsRelationOps.add(addOp);
                            } else {
                                successOps.add(addOp);
                                successOps.add(delOp);
                            }
                        }
                        // 先减件后加件
                        else {
                            if (itemCodeRelationMap.containsKey(addOp.getItemCode())) {
                                successOps.add(addOp);
                                successOps.add(delOp);
                            } else {
                                notExistsRelationOps.add(delOp);
                            }
                        }
                        break;
                    }
                }
            });
        }
        if (!addOps.isEmpty()) {
            List<PackageItemOp> restAddOps = new ArrayList<>(addOps);
            restAddOps.removeAll(successOps);
            restAddOps.removeAll(existsRelationOps);
            restAddOps.forEach(op -> {
                if (itemCodeRelationMap.containsKey(op.getItemCode())) {
                    existsRelationOps.add(op);
                }
            });
        }
        if (!deleteOps.isEmpty()) {
            List<PackageItemOp> restDeleteOps = new ArrayList<>(deleteOps);
            restDeleteOps.removeAll(successOps);
            restDeleteOps.removeAll(notExistsRelationOps);
            restDeleteOps.forEach(op -> {
                if (!itemCodeRelationMap.containsKey(op.getItemCode())) {
                    notExistsRelationOps.add(op);
                }
            });
        }

        putStatus.apply(6, puckIdFunc.apply(existsRelationOps));
        putStatus.apply(7, puckIdFunc.apply(notExistsRelationOps));
        addOps.removeAll(successOps);
        deleteOps.removeAll(successOps);
        addOps.removeAll(existsRelationOps);
        deleteOps.removeAll(notExistsRelationOps);

        // 删除集包快件关联
        packageItemRelRepo.deleteAll(deleteOps.stream()
            .map(op -> itemCodeRelationMap.get(op.getItemCode()))
            .collect(Collectors.toList()));

        // 前置条件：之前应处理了操作的时间顺序问题。
        // 增加集包快件关联
        packageItemRelRepo.saveAll(addOps.stream()
            .map(op -> PackageItemRel.builder()
                .packageCode(op.getPackageCode())
                .itemCode(op.getItemCode())
                .operator(op.getOperator())
                .createAt(op.getOpTime()).build())
            .collect(Collectors.toList()));

        List<Item> items = new ArrayList<>();

        // 结束操作验证，合并分类操作
        ops = new ArrayList<>();
        ops.addAll(addOps);
        ops.addAll(deleteOps);

        successOps.addAll(ops);
        putStatus.apply(0, puckIdFunc.apply(successOps));

        ops.forEach(op -> {
            // 只保留验证成功的操作的快件
            items.add(itemMap.get(op.getItemCode()));
            // 为新增，设置id为空
            op.setId(null);
        });

        // 保存操作记录
        packageItemOpRepo.saveAll(ops);

        // 修改快件状态
        addOps.forEach(op -> {
            itemMap.get(op.getItemCode()).setPackTime(op.getOpTime());
        });
        deleteOps.forEach(op -> {
            itemMap.get(op.getItemCode()).setPackTime(null);
        });

        itemRepo.saveAll(items);

        return result;
    }

    public Result addOpRecord(PackageItemOp op) {
        op.setOperator(SessionUserUtils.getUser().getId());
        op.setOpTime(new Date());
        op = packageItemOpRepo.save(op);
        return Result.from(op != null);
    }
}
package sorting.api.packages;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import sorting.api.codedaddress.CodedAddressRepo;
import sorting.api.codedaddress.QCodedAddress;
import sorting.api.common.Page;
import sorting.api.common.PageParams;
import sorting.api.common.PageUtils;
import sorting.api.common.Result;
import sorting.api.item.Item;
import sorting.api.item.ItemRepo;
import sorting.api.item.QItem;
import sorting.api.packageitem.*;
import sorting.api.packages.deleted.DeletedPackage;
import sorting.api.packages.deleted.DeletedPackageRepo;
import sorting.api.user.QUser;
import sorting.api.user.SessionUserUtils;
import sorting.api.user.UserRepo;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequestMapping("/package")
@RestController
public class PackageController {
    @Autowired
    private PackageRepo packageRepo;
    @Autowired
    private ItemRepo itemRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PackageItemRelRepo packageItemRelRepo;
    @Autowired
    private PackageItemOpRepo packageItemOpRepo;
    @Autowired
    private DeletedPackageRepo deletedPackageRepo;
    @Autowired
    private CodedAddressRepo codedAddressRepo;
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/page")
    public Page<PackageInfo> queryPage(@RequestParam Map<String, String> params, PageParams pageParams) {
        QPackage qPackage = QPackage.package$;
        QUser qUser = QUser.user;
        QCodedAddress qCodedAddress = QCodedAddress.codedAddress;
        JPAQuery<?> query = new JPAQuery<>(entityManager)
            .select(Projections.bean(
                PackageInfo.class,
                qPackage.code, qPackage.destCode, qPackage.createAt, qPackage.operator,
                qCodedAddress.address.as("destAddress"),
                qUser.name.as("operatorName"), qUser.phone.as("operatorPhone")))
            .from(qPackage, qUser, qCodedAddress)
            .where(qPackage.operator.eq(qUser.id).and(qPackage.destCode.eq(qCodedAddress.code)));

        if (!StringUtils.equals(params.get("fromAll"), "1")) {
            query.where(qPackage.operator.eq(SessionUserUtils.getUser().getId()));
        }
        if (StringUtils.isNotEmpty(params.get("code"))) {
            query.where(qPackage.code.like(params.get("code") + "%"));
        }
        query.orderBy(qPackage.createAt.desc());
        return PageUtils.fetchPage(query, pageParams);
    }

    @GetMapping("/details")
    public Map<String, Object> queryPackageDetails(String code) {
        Map<String, Object> details = new HashMap<>();

        Package pkg = packageRepo.findById(code).get();
        details.put("package", pkg);

        userRepo.findById(pkg.getOperator()).ifPresent(user -> details.put("creator", user));
        codedAddressRepo.findById(pkg.getDestCode()).ifPresent(address -> details.put("destAddress", address));

        return details;
    }

    @PostMapping("/batch")
    public Result batchAdd(@RequestBody List<Package> packages, SmartCreateSpec smartCreateSpec) {
        Map<Integer, Collection<String>> addStatus = new HashMap<>();
        BiFunction<Integer, List<String>, List<String>> putStatus = (status, itemCodes) -> {
            if (!itemCodes.isEmpty()) {
                addStatus.put(status, itemCodes);
            }
            return itemCodes;
        };

        Function<List<Package>, List<String>> puckCodeFunc = pkgs ->
            pkgs.stream().map(Package::getCode).collect(Collectors.toList());

        List<String> codes = puckCodeFunc.apply(packages);
        // 查询已存在集包库中的集包编码
        QPackage qPackage = QPackage.package$;
        List<String> existsCodes = new JPAQuery<>(entityManager)
            .select(qPackage.code)
            .from(qPackage)
            .where(qPackage.code.in(codes))
            .fetchResults().getResults();
        putStatus.apply(2, existsCodes);//已存在
        // 只保留不存在集包库中的集包（新的）
        codes.removeAll(existsCodes);
        if (codes.isEmpty()) {
            return Result.ok(addStatus);
        }
        List<Package> newPackages = codes.stream()
            .map(code -> packages.stream().filter(pkg -> pkg.getCode().equals(code)).findFirst().get())
            .collect(Collectors.toList());

        Set<String> destCodes = newPackages.stream().map(Package::getDestCode).collect(Collectors.toSet());
        // 查询已存在目标地址库中的目的地编码
        QCodedAddress qCodedAddress = QCodedAddress.codedAddress;
        List<String> existsDestCodes = new JPAQuery<>(entityManager)
            .select(qCodedAddress.code)
            .from(qCodedAddress)
            .where(qCodedAddress.code.in(destCodes))
            .fetchResults().getResults();

        List<Package> canAddPackages = existsDestCodes.isEmpty()
            ? null
            : newPackages.stream()
                .filter(pkg -> existsDestCodes.stream().anyMatch(destCode -> destCode.equals(pkg.getDestCode())))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(canAddPackages)) {
            newPackages.removeAll(canAddPackages);
        }
        putStatus.apply(3, puckCodeFunc.apply(newPackages));//目的地无效
        if (!CollectionUtils.isEmpty(canAddPackages)) {
            canAddPackages = (List<Package>) packageRepo.saveAll(canAddPackages);
            putStatus.apply(0, puckCodeFunc.apply(canAddPackages));

            if (smartCreateSpec.isSmartCreate()) {
                batchSmartAllocItems(canAddPackages, smartCreateSpec);
            }
        }

        return Result.ok(addStatus);
    }

    @PostMapping("/batch_delete")
    @Transactional
    public Result batchDelete(@RequestBody List<DeletedPackage> packages) {
        final Result result = Result.ok(new HashMap<Integer, List<String>>());
        BiFunction<Integer, List<String>, List<String>> putStatus = (status, itemCodes) -> {
            if (!itemCodes.isEmpty()) {
                ((Map<Integer, List<String>>)result.getData()).put(status, itemCodes);
            }
            return itemCodes;
        };

        List<String> packageCodes = packages.stream().map(DeletedPackage::getCode).collect(Collectors.toList());
        // 查询已存在集包库中的集包编码和关联快件的数量
        QPackage qPackage = QPackage.package$;
        QPackageItemRel qPackageItemRel = QPackageItemRel.packageItemRel;
        Map<String, Tuple> existsPackageInfoMap = new HashMap<>();
        List<String> hasItemsPackageCodes = new ArrayList<>(); //已关联快件的
        new JPAQuery<>(entityManager)
            .select(
                Projections.tuple(
                    qPackage.code,
                    qPackage.destCode,
                    qPackage.operator,
                    qPackage.createAt,
                    JPAExpressions
                        .select(qPackageItemRel.itemCode.count())
                        .from(qPackageItemRel)
                        .where(qPackageItemRel.packageCode.eq(qPackage.code))))
            .from(qPackage)
            .where(qPackage.code.in(packageCodes))
            .fetchResults().getResults()
            .forEach(stat -> {
                String packageCode = stat.get(0, String.class);
                int items = stat.get(4, Long.class).intValue();
                existsPackageInfoMap.put(packageCode, stat);
                if (items > 0) {
                    hasItemsPackageCodes.add(packageCode);
                }
            });
        putStatus.apply(2, hasItemsPackageCodes);
        putStatus.apply(3, packageCodes.stream()
            .filter(code -> !existsPackageInfoMap.containsKey(code))
            .collect(Collectors.toList()));
        packageCodes.removeIf(code -> !existsPackageInfoMap.containsKey(code));
        packageCodes.removeAll(hasItemsPackageCodes);
        if (packageCodes.isEmpty()) {
            return result;
        }

        packages.removeIf(pkg -> !packageCodes.contains(pkg.getCode()));
        List<DeletedPackage> deletedPackages = packages.stream().map(pkg -> {
            pkg.setDestCode(existsPackageInfoMap.get(pkg.getCode()).get(1, String.class));
            pkg.setCreator(existsPackageInfoMap.get(pkg.getCode()).get(2, Long.class));
            pkg.setCreateAt(existsPackageInfoMap.get(pkg.getCode()).get(3, Date.class));
            return pkg;
        }).collect(Collectors.toList());
        deletedPackageRepo.saveAll(deletedPackages);

        packageRepo.deleteAll(packages.stream()
            .map(delPkg -> {
                Package pkg = new Package();
                pkg.setCode(delPkg.getCode());
                return pkg;
            })
            .collect(Collectors.toList()));

        putStatus.apply(0, packageCodes);

        return result;
    }

    @PostMapping
    public Result add(@RequestBody Package pkg, SmartCreateSpec smartCreateSpec, Long schemeId) {
        if (packageRepo.existsById(pkg.getCode())) {
            return Result.fail(2).message("集包已存在");
        }
        if (!codedAddressRepo.existsById(pkg.getDestCode())) {
            return Result.fail(3).message("未查询到目的地");
        }

        pkg.setOperator(SessionUserUtils.getUser().getId());
        pkg.setCreateAt(new Date());
        pkg = packageRepo.save(pkg);

        if (pkg != null && smartCreateSpec.isSmartCreate()) {
            smartAllocItems(pkg, smartCreateSpec);
        }

        return Result.from(pkg != null);
    }

    @Transactional
    @DeleteMapping
    public Result delete(String code) {
        Optional<Package> pkgOpt = packageRepo.findById(code);
        if (!pkgOpt.isPresent()) {
            return Result.fail(2).message("不存在的集包");
        }
        if (packageItemRelRepo.existsByPackageCode(code)) {
            return Result.fail(3).message("集包包含快件，不能删除");
        }
        Package pkg = pkgOpt.get();
        packageRepo.deleteById(code);
        DeletedPackage deletedPackage = new DeletedPackage();
        deletedPackage.setCode(pkg.getCode());
        deletedPackage.setDestCode(pkg.getDestCode());
        deletedPackage.setCreator(pkg.getOperator());
        deletedPackage.setOperator(SessionUserUtils.getUser().getId());
        deletedPackage.setCreateAt(pkg.getCreateAt());
        deletedPackage.setDeleteAt(new Date());
        deletedPackage = deletedPackageRepo.save(deletedPackage);
        return Result.from(deletedPackage != null);
    }

    @Transactional
    public Result smartAllocItems(Package pkg, SmartCreateSpec smartCreateSpec) {
        // 查询出相同目的地并且还未分配的快件
        QItem qItem = QItem.item;
        List<Item> items = new JPAQueryFactory(entityManager).selectFrom(qItem)
            .where(qItem.packTime.isNull().and(qItem.destCode.eq(pkg.getDestCode())))
            .offset(0)
            .limit(smartCreateSpec.getAllocItemNumMax())
            .fetchResults()
            .getResults();
        items.sort(Comparator.comparing(Item::getCreateAt));

        if (items.isEmpty()) {
            return Result.ok();
        }

        List<PackageItemRel> relations = new ArrayList<>();  // 集包快件关联记录
        List<PackageItemOp> opRecords = new ArrayList<>(); //集包增加快件记录

        for (Item item : items) {
            item.setPackTime(pkg.getCreateAt());

            PackageItemRel rel = new PackageItemRel();
            rel.setPackageCode(pkg.getCode());
            rel.setItemCode(item.getCode());
            rel.setOperator(pkg.getOperator());
            rel.setCreateAt(pkg.getCreateAt());
            relations.add(rel);

            PackageItemOp op = new PackageItemOp();
            op.setPackageCode(pkg.getCode());
            op.setItemCode(item.getCode());
            op.setOperator(pkg.getOperator());
            op.setOpTime(pkg.getCreateAt());
            op.setOpType(1);
            opRecords.add(op);
        }
        packageItemRelRepo.saveAll(relations);
        packageItemOpRepo.saveAll(opRecords);
        itemRepo.saveAll(items);
        return Result.ok();
    }

    @Transactional
    public Result batchSmartAllocItems(List<Package> packages, SmartCreateSpec smartCreateSpec) {
        // 查询出相同目的地并且还未分配的快件
        Map<String, Stack<Item>> itemsMap = new HashMap<>();
        QItem qItem = QItem.item;
        List<Item> items = new JPAQueryFactory(entityManager).selectFrom(qItem)
            .where(qItem.packTime.isNull()
                .and(qItem.destCode.in(packages.stream().map(Package::getDestCode).collect(Collectors.toSet()))))
            .offset(0)
            .limit(smartCreateSpec.getAllocItemNumMax() * packages.size())
            .fetchResults()
            .getResults();
        items.forEach(item -> {
            Stack<Item> itemStack = itemsMap.get(item.getDestCode());
            if (itemStack == null) {
                itemStack = new Stack<>();
                itemsMap.put(item.getDestCode(), itemStack);
            }
            itemStack.push(item);
        });

        if (itemsMap.isEmpty()) {
            return Result.ok();
        }

        Date now = new Date();
        List<PackageItemRel> relations = new ArrayList<>();  // 集包快件关联记录
        List<PackageItemOp> opRecords = new ArrayList<>(); //集包增加快件记录

        packages.sort(Comparator.comparing(Package::getCreateAt));//按创建时间升序排序,早创建的集包先分配快件
        for (Package pkg : packages) {
            for (int n = 0; n < smartCreateSpec.getAllocItemNumMax(); n++) {
                Stack<Item> sameDestItems = itemsMap.get(pkg.getDestCode());
                if (sameDestItems == null || sameDestItems.isEmpty()) {
                    break;
                }
                Item item = sameDestItems.pop();

                item.setPackTime(now);

                PackageItemRel rel = new PackageItemRel();
                rel.setPackageCode(pkg.getCode());
                rel.setItemCode(item.getCode());
                rel.setOperator(pkg.getOperator());
                rel.setCreateAt(now);
                relations.add(rel);

                PackageItemOp op = new PackageItemOp();
                op.setPackageCode(pkg.getCode());
                op.setItemCode(item.getCode());
                op.setOperator(pkg.getOperator());
                op.setOpTime(now);
                op.setOpType(1);
                opRecords.add(op);
            }
        }

        packageItemRelRepo.saveAll(relations);
        packageItemOpRepo.saveAll(opRecords);
        itemRepo.saveAll(items);
        return Result.ok();
    }
}

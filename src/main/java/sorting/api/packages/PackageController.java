package sorting.api.packages;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import sorting.api.codedaddress.CodedAddressRepo;
import sorting.api.codedaddress.QCodedAddress;
import sorting.api.common.Page;
import sorting.api.common.PageParams;
import sorting.api.common.PageUtils;
import sorting.api.common.Result;
import sorting.api.item.Item;
import sorting.api.item.QItem;
import sorting.api.packages.deleted.DeletedPackage;
import sorting.api.packages.deleted.DeletedPackageRepo;
import sorting.api.user.QUser;
import sorting.api.user.SessionUserUtils;
import sorting.api.user.UserRepo;

import javax.persistence.EntityManager;
import java.util.*;

@RequestMapping("/package")
@RestController
public class PackageController {
    @Autowired
    private PackageRepo packageRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PackageItemRelRepo packageItemRelRepo;
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

        QPackageItemRel qPackageItemRel = QPackageItemRel.packageItemRel;
        QItem qItem = QItem.item;
        List<Item> items = new JPAQuery<Item>(entityManager)
            .select(qItem)
            .from(qItem, qPackageItemRel)
            .where(qPackageItemRel.itemCode.eq(qItem.code))
            .where(qPackageItemRel.packageCode.eq(code))
            .fetchResults()
            .getResults();
        details.put("items", items);

        return details;
    }

    @PostMapping
    public Result add(@RequestBody Package pkg) {
        if (packageRepo.existsById(pkg.getCode())) {
            return Result.fail().message("包裹编号重复");
        }
        if (!codedAddressRepo.existsById(pkg.getDestCode())) {
            return Result.fail().message("未查询到目的地");
        }
        pkg.setOperator(SessionUserUtils.getUser().getId());
        pkg.setCreateAt(new Date());
        pkg = packageRepo.save(pkg);
        return Result.from(pkg != null);
    }

    @Transactional
    @DeleteMapping
    public Result delete(String code) {
        Optional<Package> pkgOpt = packageRepo.findById(code);
        if (!pkgOpt.isPresent()) {
            return Result.fail().message("不存在的包裹");
        }
        Package pkg = pkgOpt.get();
        packageRepo.deleteById(code);
        packageItemRelRepo.deleteByPackageCode(code);
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
}

package sorting.api.packages;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sorting.api.codedaddress.CodedAddress;
import sorting.api.codedaddress.CodedAddressRepo;
import sorting.api.common.Page;
import sorting.api.common.PageParams;
import sorting.api.common.PageUtils;
import sorting.api.common.Result;
import sorting.api.item.Item;
import sorting.api.item.QItem;
import sorting.api.user.SessionUserUtils;
import sorting.api.user.User;
import sorting.api.user.UserRepo;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/package")
@RestController
public class PackageController {
    @Autowired
    private PackageRepo packageRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private CodedAddressRepo codedAddressRepo;
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/page")
    public Page<Package> queryPage(@RequestParam Map<String, String> params, PageParams pageParams) {
        QPackage qPackage = QPackage.package$;
        JPAQuery<?> query = new JPAQueryFactory(entityManager).selectFrom(qPackage);

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

        User operator = userRepo.findById(pkg.getOperator()).get();
        details.put("creator", operator);

        CodedAddress address = codedAddressRepo.findById(pkg.getDestCode()).orElse(null);
        if (address != null) {
            details.put("destAddress", address);
        }

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
            return Result.fail().message("目的地编号不存在");
        }
        pkg.setOperator(SessionUserUtils.getUser().getId());
        pkg.setCreateAt(new Date());
        pkg = packageRepo.save(pkg);
        return Result.from(pkg != null);
    }

    @DeleteMapping
    public Result delete(@RequestBody Package pkg) {
        packageRepo.delete(pkg);
        return Result.ok();
    }
}

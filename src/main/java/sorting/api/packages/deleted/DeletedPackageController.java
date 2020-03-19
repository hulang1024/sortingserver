package sorting.api.packages.deleted;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import sorting.api.common.Page;
import sorting.api.common.PageParams;
import sorting.api.common.PageUtils;
import sorting.api.common.Result;
import sorting.api.item.Item;
import sorting.api.item.QItem;
import sorting.api.packages.*;
import sorting.api.packages.Package;
import sorting.api.user.SessionUserUtils;
import sorting.api.user.User;
import sorting.api.user.UserRepo;

import javax.persistence.EntityManager;
import java.util.*;

@RequestMapping("/deleted_package")
@RestController
public class DeletedPackageController {
    @Autowired
    private PackageRepo packageRepo;
    @Autowired
    private PackageItemRelRepo packageItemRelRepo;
    @Autowired
    private DeletedPackageRepo deletedPackageRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/page")
    public Page<Package> queryPage(@RequestParam Map<String, String> params, PageParams pageParams) {
        QDeletedPackage qPackage = QDeletedPackage.deletedPackage;
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

        DeletedPackage pkg = deletedPackageRepo.findById(code).get();
        details.put("package", pkg);

        User creator = userRepo.findById(pkg.getOperator()).get();
        details.put("creator", creator);
        User deleteOperator = userRepo.findById(pkg.getOperator()).get();
        details.put("deleteOperator", deleteOperator);
        return details;
    }

    @Transactional
    @PostMapping
    public Result add(String code) {
        Optional<Package> pkgOpt = packageRepo.findById(code);
        if (!pkgOpt.isPresent()) {
            return Result.fail().message("不存在的包裹");
        }
        Package pkg = pkgOpt.get();
        packageItemRelRepo.deleteByPackageCode(code);
        DeletedPackage deletedPackage = new DeletedPackage();
        deletedPackage.setCode(pkg.getCode());
        deletedPackage.setDestCode(pkg.getDestCode());
        deletedPackage.setCreator(pkg.getOperator());
        deletedPackage.setOperator(SessionUserUtils.getUser().getId());
        deletedPackage.setCreateAt(new Date());
        deletedPackage = deletedPackageRepo.save(deletedPackage);
        return Result.from(deletedPackage != null);
    }
}

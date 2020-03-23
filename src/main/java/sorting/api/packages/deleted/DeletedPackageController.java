package sorting.api.packages.deleted;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sorting.api.codedaddress.CodedAddressRepo;
import sorting.api.common.Page;
import sorting.api.common.PageParams;
import sorting.api.common.PageUtils;
import sorting.api.packages.Package;
import sorting.api.user.SessionUserUtils;
import sorting.api.user.UserRepo;

import javax.persistence.EntityManager;
import java.util.*;

@RequestMapping("/deleted_package")
@RestController
public class DeletedPackageController {
    @Autowired
    private DeletedPackageRepo deletedPackageRepo;
    @Autowired
    private CodedAddressRepo codedAddressRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/page")
    public Page<DeletedPackage> queryPage(@RequestParam Map<String, String> params, PageParams pageParams) {
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
        userRepo.findById(pkg.getOperator()).ifPresent(user -> details.put("creator", user));
        codedAddressRepo.findById(pkg.getDestCode()).ifPresent(address -> details.put("destAddress", address));
        userRepo.findById(pkg.getOperator()).ifPresent(user -> details.put("deleteOperator", user));

        return details;
    }
}

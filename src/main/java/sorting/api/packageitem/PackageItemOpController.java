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
import sorting.api.packages.*;
import sorting.api.scheme.Scheme;
import sorting.api.scheme.SchemeRepo;
import sorting.api.user.QUser;
import sorting.api.user.SessionUserUtils;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

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
        if (!packageRepo.existsById(packageCode)) {
            return Result.fail(1).message("未查询到集包");
        }
        Optional<Item> itemOpt = itemRepo.findById(itemCode);
        if (!itemOpt.isPresent()) {
            return Result.fail(4).message("未查询到快件");
        }
        Optional<Scheme> schemeOpt = schemeRepo.findById(schemeId);
        if (!schemeOpt.isPresent()) {
            return Result.fail(2).message("未知模式");
        }
        if (!itemCode.matches(schemeOpt.get().getItemCodePattern())) {
            return Result.fail(3).message("快件编号有误");
        }
        Optional<PackageItemRel> relOpt = packageItemRelRepo.findByItemCode(itemCode);
        if (relOpt.isPresent()) {
            if (relOpt.get().getPackageCode().equals(packageCode)) {
                return Result.fail(5).message("快件早已加到集包");
            } else {
                return Result.fail(5).message("快件早已加到其它集包");
            }
        }

        itemOpt.get().setPackTime(new Date());
        itemRepo.save(itemOpt.get());

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


    public Result addOpRecord(PackageItemOp op) {
        op.setOperator(SessionUserUtils.getUser().getId());
        op.setOpTime(new Date());
        op = packageItemOpRepo.save(op);
        return Result.from(op != null);
    }
}
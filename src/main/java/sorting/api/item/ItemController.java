package sorting.api.item;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sorting.api.codedaddress.CodedAddressRepo;
import sorting.api.codedaddress.QCodedAddress;
import sorting.api.common.Page;
import sorting.api.common.PageParams;
import sorting.api.common.PageUtils;
import sorting.api.packageitem.PackageItemRelRepo;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

@RequestMapping("/item")
@RestController
public class ItemController {
    @Autowired
    private ItemRepo itemRepo;
    @Autowired
    private PackageItemRelRepo packageItemRelRepo;
    @Autowired
    private CodedAddressRepo codedAddressRepo;
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/page")
    public Page<ItemInfo> queryPage(@RequestParam Map<String, String> params, PageParams pageParams) {
        QItem qItem = QItem.item;
        QCodedAddress qCodedAddress = QCodedAddress.codedAddress;

        JPAQuery<?> query = new JPAQuery<>(entityManager)
            .select(Projections.bean(
                ItemInfo.class,
                qItem.code, qItem.destCode, qItem.createAt, qItem.packTime,
                qCodedAddress.address.as("destAddress")))
            .from(qItem, qCodedAddress)
            .where(qItem.destCode.eq(qCodedAddress.code));

        if (StringUtils.isNotEmpty(params.get("code"))) {
            query.where(qItem.code.like(params.get("code") + "%"));
        }
        query.orderBy(qItem.createAt.desc());
        return PageUtils.fetchPage(query, pageParams);
    }

    @GetMapping("/details")
    public Map<String, Object> queryItemDetails(String code) {
        Map<String, Object> details = new HashMap<>();
        Item item = itemRepo.findById(code).get();
        details.put("item", item);
        codedAddressRepo.findById(item.getDestCode()).ifPresent(address -> details.put("destAddress", address));
        if (item.getPackTime() != null) {
            packageItemRelRepo.findByItemCode(code).ifPresent(rel -> details.put("packageCode", rel.getPackageCode()));
        }

        return details;
    }

}

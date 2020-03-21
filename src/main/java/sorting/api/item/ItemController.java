package sorting.api.item;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sorting.api.codedaddress.CodedAddress;
import sorting.api.codedaddress.CodedAddressRepo;
import sorting.api.codedaddress.QCodedAddress;
import sorting.api.common.Page;
import sorting.api.common.PageParams;
import sorting.api.common.PageUtils;
import sorting.api.packages.PackageInfo;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

@RequestMapping("/item")
@RestController
public class ItemController {
    @Autowired
    private ItemRepo itemRepo;
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
                PackageInfo.class,
                qItem.code, qItem.destCode, qItem.createAt,
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

        return details;
    }

}

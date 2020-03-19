package sorting.api.item;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sorting.api.common.Page;
import sorting.api.common.PageParams;
import sorting.api.common.PageUtils;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

@RequestMapping("/item")
@RestController
public class ItemController {
    @Autowired
    private ItemRepo itemRepo;
    @Autowired
    private EntityManager entityManager;

    @GetMapping("/page")
    public Page<Item> queryPage(@RequestParam Map<String, String> params, PageParams pageParams) {
        QItem qItem = QItem.item;
        JPAQuery<?> query = new JPAQueryFactory(entityManager).selectFrom(qItem);

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
        details.put("package", item);

        return details;
    }

}

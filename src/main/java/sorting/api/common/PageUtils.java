package sorting.api.common;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.PageRequest;

import java.util.List;

public class PageUtils {
    public static <T> Page<T> fetchPage(JPAQuery<?> query, PageParams pageParams) {
        QueryResults<?> results = query
                .offset((pageParams.getPage() - 1) * pageParams.getSize())
                .limit(pageParams.getSize())
                .fetchResults();
        Page<T> page = new Page<T>();
        page.setTotal(results.getTotal());
        page.setContent((List<T>)results.getResults());
        return page;
    }
}

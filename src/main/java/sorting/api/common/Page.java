package sorting.api.common;

import lombok.Data;

import java.util.List;

@Data
public class Page<T> {
    private long total;
    private List<T> content;
}

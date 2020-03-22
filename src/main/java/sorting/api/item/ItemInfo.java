package sorting.api.item;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ItemInfo extends Item {
    private String packageCode;
    private String destAddress;
}

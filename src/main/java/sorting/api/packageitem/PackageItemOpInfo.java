package sorting.api.packageitem;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PackageItemOpInfo extends PackageItemOp {
    private String operatorName;
    private String operatorPhone;
}

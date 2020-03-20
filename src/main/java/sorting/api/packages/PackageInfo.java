package sorting.api.packages;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PackageInfo extends Package {
    private String destAddress;
    private String operatorPhone;
    private String operatorName;
}

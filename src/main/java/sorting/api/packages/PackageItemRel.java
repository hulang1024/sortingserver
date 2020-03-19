package sorting.api.packages;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Data
public class PackageItemRel {
    @Id
    private Long id;
    private String packageCode;
    private String itemCode;
    private Date createAt;
    private Long operator;
}

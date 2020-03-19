package sorting.api.packages.deleted;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Table(name="package_deleted")
@Entity
@Data
public class DeletedPackage {
    @Id
    @GenericGenerator(name = "idGenerator", strategy = "assigned")
    @GeneratedValue(generator = "idGenerator")
    private String code;
    private String destCode;
    private Date createAt;
    private Long creator;
    private Long operator;
}

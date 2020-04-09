package sorting.api.packages;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Data
public class Package {
    @Id
    @GenericGenerator(name = "idGenerator", strategy = "assigned")
    @GeneratedValue(generator = "idGenerator")
    @EqualsAndHashCode.Include
    private String code;
    private String destCode;
    private Date createAt;
    private Long operator;
}
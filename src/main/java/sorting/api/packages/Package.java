package sorting.api.packages;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Data
public class Package {
    @GeneratedValue(strategy= GenerationType.AUTO)
    @Id
    private String code;
    private String destCode;
    private Date createAt;
    private Long operator;
}
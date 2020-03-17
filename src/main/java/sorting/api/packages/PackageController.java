package sorting.api.packages;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sorting.api.common.Result;
import sorting.api.user.SessionUserUtils;

import java.util.Date;

@RequestMapping("/package")
@RestController
public class PackageController {
    @Autowired
    private PackageRepo packageRepo;

    @PostMapping
    public Result add(@RequestBody Package pkg) {
        if (packageRepo.existsByCode(pkg.getCode())) {
            return Result.fail().message("包编号重复");
        }
        pkg.setOperator(SessionUserUtils.getUser().getId());
        pkg.setCreateAt(new Date());
        pkg = packageRepo.save(pkg);
        return Result.from(pkg != null);
    }

    @DeleteMapping
    public Result delete(@RequestBody Package pkg) {
        packageRepo.delete(pkg);
        return Result.ok();
    }
}

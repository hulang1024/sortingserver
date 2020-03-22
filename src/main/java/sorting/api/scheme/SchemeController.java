package sorting.api.scheme;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/scheme")
public class SchemeController {
    @Autowired
    private SchemeRepo schemeRepo;

    @GetMapping("/all")
    public List<Scheme> queryAll() {
        return Lists.newArrayList(schemeRepo.findAll());
    }
}

package sorting.api.codedaddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coded_address")
public class CodedAddressController {
    @Autowired
    private CodedAddressRepo codedAddressRepo;

    @GetMapping
    public String getAddress(String code) {
        return codedAddressRepo.findById(code).map(CodedAddress::getAddress).orElse(null);
    }
}
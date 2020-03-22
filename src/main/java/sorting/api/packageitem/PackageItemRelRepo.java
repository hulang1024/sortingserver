package sorting.api.packageitem;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface PackageItemRelRepo extends CrudRepository<PackageItemRel, String> {
    void deleteByPackageCode(String s);
    void deleteByPackageCodeAndItemCode(String packageCode, String itemCode);
    boolean existsByPackageCodeAndItemCode(String packageCode, String itemCode);
    Optional<PackageItemRel> findByItemCode(String itemCode);
}

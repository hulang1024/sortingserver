package sorting.api.packages;

import org.springframework.data.repository.CrudRepository;

public interface PackageItemRelRepo extends CrudRepository<PackageItemRel, String> {
    void deleteByPackageCode(String s);
}

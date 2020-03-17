package sorting.api.packages;

import org.springframework.data.repository.CrudRepository;
import sorting.api.user.User;

import java.util.Optional;

public interface PackageRepo extends CrudRepository<Package, String> {
    Optional<Package> findByCode(String code);
    boolean existsByCode(String code);
}

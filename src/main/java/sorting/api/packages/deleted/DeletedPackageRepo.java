package sorting.api.packages.deleted;

import org.springframework.data.repository.CrudRepository;
import sorting.api.packages.Package;

public interface DeletedPackageRepo extends CrudRepository<DeletedPackage, String> {
}

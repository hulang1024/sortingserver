package sorting.api.user;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepo extends CrudRepository<User, Long> {
    Optional<User> findByPhoneOrCode(String phone, String code);
    boolean existsByCode(String code);
    boolean existsByPhone(String phone);
    Optional<User> findTopByOrderByCreateAtDesc();
}

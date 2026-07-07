package fu.se.smms.repository;

import fu.se.smms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByResetPasswordToken(String resetPasswordToken);

    // Search staff by name or phone (staff must have a role)
    @Query("SELECT u FROM User u WHERE u.role IS NOT NULL AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchStaffByKeyword(@Param("keyword") String keyword);
}

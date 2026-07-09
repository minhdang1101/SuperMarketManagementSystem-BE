package fu.se.smms.repository;

import fu.se.smms.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    boolean existsByName(String name);

    boolean existsByNameAndCategoryIdNot(String name, Integer categoryId);

    List<Category> findByNameContainingIgnoreCase(String keyword);
}

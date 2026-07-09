package fu.se.smms.repository;

import fu.se.smms.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    @Query("""
            SELECT s FROM Supplier s
            WHERE (:keyword IS NULL
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR s.phone LIKE CONCAT('%', :keyword, '%')
                   OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR s.status = :status)
            """)
    Page<Supplier> search(@Param("keyword") String keyword,
                          @Param("status") Boolean status,
                          Pageable pageable);
}

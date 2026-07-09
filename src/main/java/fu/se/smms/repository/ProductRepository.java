package fu.se.smms.repository;

import fu.se.smms.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query("""
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            LEFT JOIN FETCH p.supplier
            WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
              AND (:supplierId IS NULL OR p.supplier.supplierId = :supplierId)
              AND (:status IS NULL OR p.status = :status)
            """)
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("categoryId") Integer categoryId,
                         @Param("supplierId") Integer supplierId,
                         @Param("status") Boolean status,
                         Pageable pageable);

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcodeAndProductIdNot(String barcode, Integer productId);

    boolean existsByBarcode(String barcode);


    @Query(value = """
            SELECT p FROM Product p
            LEFT JOIN FETCH p.category
            WHERE p.status = true
              AND (:query IS NULL OR :query = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
            """,
           countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.status = true
              AND (:query IS NULL OR :query = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
            """)
    Page<Product> searchForPOS(@Param("query") String query,
                                @Param("categoryId") Integer categoryId,
                                Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId IN :ids")
    List<Product> findAllByIdWithLock(@Param("ids") List<Integer> ids);


    @Modifying
    @Query("UPDATE Product p SET p.stockLevel = p.stockLevel - :quantity WHERE p.productId = :productId AND p.stockLevel >= :quantity")
    int decreaseStockLevel(@Param("productId") Integer productId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stockLevel = p.stockLevel + :quantity WHERE p.productId = :productId")
    void increaseStockLevel(@Param("productId") Integer productId, @Param("quantity") Integer quantity);


    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = true AND p.stockLevel < p.minStockLevel")
    Integer countLowStockProducts();


    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE LOWER(p.barcode) = LOWER(:barcode) AND p.status = true")
    Optional<Product> findByBarcodeIgnoreCase(@Param("barcode") String barcode);
}

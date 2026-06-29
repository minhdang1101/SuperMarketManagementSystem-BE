package fu.se.smms.repository;

import fu.se.smms.entity.StockAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Integer> {

    @Query("""
            SELECT sa.product.productId, SUM(sa.quantity)
            FROM StockAdjustment sa
            GROUP BY sa.product.productId
            """)
    List<Object[]> sumAdjustmentGroupByProduct();

    @Query("""
            SELECT COALESCE(SUM(sa.quantity), 0)
            FROM StockAdjustment sa
            WHERE sa.product.productId = :productId
            """)
    Integer sumAdjustmentByProductId(@Param("productId") Integer productId);

    @Query("""
            SELECT sa FROM StockAdjustment sa
            JOIN FETCH sa.product
            JOIN FETCH sa.adjustedBy
            WHERE (:productId IS NULL OR sa.product.productId = :productId)
            """)
    Page<StockAdjustment> findByProductId(@Param("productId") Integer productId, Pageable pageable);

    @Query("""
            SELECT sa.product.productId, SUM(sa.quantity)
            FROM StockAdjustment sa
            WHERE sa.adjustedAt >= :dateFrom
              AND sa.adjustedAt <= :dateTo
              AND (:categoryId IS NULL OR sa.product.category.categoryId = :categoryId)
            GROUP BY sa.product.productId
            """)
    List<Object[]> sumAdjustmentByDateRange(@Param("dateFrom") LocalDateTime dateFrom,
                                            @Param("dateTo") LocalDateTime dateTo,
                                            @Param("categoryId") Integer categoryId);
}

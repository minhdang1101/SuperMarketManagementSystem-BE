package fu.se.smms.repository;

import fu.se.smms.entity.GoodsReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface GoodsReceiptDetailRepository extends JpaRepository<GoodsReceiptDetail, Integer> {

    @Query("""
            SELECT grd.product.productId, SUM(grd.receivedQuantity)
            FROM GoodsReceiptDetail grd
            WHERE grd.goodsReceipt.status = fu.se.smms.enums.GoodsReceiptStatus.COMPLETED
            GROUP BY grd.product.productId
            """)
    List<Object[]> sumReceivedQuantityGroupByProduct();

    @Query("""
            SELECT COALESCE(SUM(grd.receivedQuantity), 0)
            FROM GoodsReceiptDetail grd
            WHERE grd.goodsReceipt.status = fu.se.smms.enums.GoodsReceiptStatus.COMPLETED
              AND grd.product.productId = :productId
            """)
    Integer sumReceivedQuantityByProductId(@Param("productId") Integer productId);

    @Query("""
            SELECT grd.product.productId, SUM(grd.receivedQuantity)
            FROM GoodsReceiptDetail grd
            WHERE grd.goodsReceipt.status = fu.se.smms.enums.GoodsReceiptStatus.COMPLETED
              AND grd.goodsReceipt.receivedDate >= :dateFrom
              AND grd.goodsReceipt.receivedDate <= :dateTo
              AND (:categoryId IS NULL OR grd.product.category.categoryId = :categoryId)
            GROUP BY grd.product.productId
            """)
    List<Object[]> sumReceivedQuantityByDateRange(@Param("dateFrom") LocalDateTime dateFrom,
                                                  @Param("dateTo") LocalDateTime dateTo,
                                                  @Param("categoryId") Integer categoryId);

    @Query("""
            SELECT grd FROM GoodsReceiptDetail grd
            JOIN FETCH grd.product
            WHERE grd.goodsReceipt.status = fu.se.smms.enums.GoodsReceiptStatus.COMPLETED
              AND grd.expiryDate IS NOT NULL
              AND grd.expiryDate <= :thresholdDate
            """)
    List<GoodsReceiptDetail> findExpiredOrExpiringSoon(@Param("thresholdDate") LocalDate thresholdDate);

    @Query("""
            SELECT COUNT(DISTINCT grd.product.productId)
            FROM GoodsReceiptDetail grd
            WHERE grd.goodsReceipt.status = fu.se.smms.enums.GoodsReceiptStatus.COMPLETED
              AND grd.expiryDate IS NOT NULL
              AND grd.expiryDate <= :thresholdDate
              AND grd.expiryDate > CURRENT_DATE
            """)
    Integer countExpiringProducts(@Param("thresholdDate") LocalDate thresholdDate);
}

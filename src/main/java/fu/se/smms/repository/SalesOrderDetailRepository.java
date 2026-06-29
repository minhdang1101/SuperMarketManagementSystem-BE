package fu.se.smms.repository;

import fu.se.smms.entity.SalesOrderDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SalesOrderDetailRepository extends JpaRepository<SalesOrderDetail, Integer> {

    @Query("""
            SELECT sod.product.productId, SUM(sod.quantity)
            FROM SalesOrderDetail sod
            GROUP BY sod.product.productId
            """)
    List<Object[]> sumSoldQuantityGroupByProduct();

    @Query("""
            SELECT COALESCE(SUM(sod.quantity), 0)
            FROM SalesOrderDetail sod
            WHERE sod.product.productId = :productId
            """)
    Integer sumSoldQuantityByProductId(@Param("productId") Integer productId);

    @Query("""
            SELECT sod.product.productId, SUM(sod.quantity)
            FROM SalesOrderDetail sod
            WHERE sod.salesOrder.orderDate >= :dateFrom
              AND sod.salesOrder.orderDate <= :dateTo
              AND (:categoryId IS NULL OR sod.product.category.categoryId = :categoryId)
            GROUP BY sod.product.productId
            """)
    List<Object[]> sumSoldQuantityByDateRange(@Param("dateFrom") LocalDateTime dateFrom,
                                              @Param("dateTo") LocalDateTime dateTo,
                                              @Param("categoryId") Integer categoryId);

    List<SalesOrderDetail> findBySalesOrder_SalesOrderId(Integer salesOrderId);

    @Query("""
            SELECT COALESCE(SUM(
                CASE 
                    WHEN sod.costPrice IS NULL THEN 0
                    ELSE sod.costPrice * sod.quantity
                END
            ), 0)
            FROM SalesOrderDetail sod
            JOIN sod.salesOrder so
            WHERE so.orderDate >= :startDate AND so.orderDate < :endDate
            """)
    BigDecimal findTotalCogs(@Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT p.category.name,
                   COALESCE(SUM(sod.totalPrice), 0),
                   COALESCE(SUM(sod.costPrice * sod.quantity), 0),
                   COUNT(DISTINCT so.salesOrderId)
            FROM SalesOrderDetail sod
            JOIN sod.product p
            JOIN sod.salesOrder so
            WHERE so.orderDate >= :startDate AND so.orderDate < :endDate
            GROUP BY p.category.categoryId, p.category.name
            ORDER BY SUM(sod.totalPrice) DESC
            """)
    List<Object[]> findRevenueGroupByCategory(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT p.name,
                   COALESCE(SUM(sod.totalPrice), 0),
                   COALESCE(SUM(sod.costPrice * sod.quantity), 0),
                   COUNT(DISTINCT so.salesOrderId)
            FROM SalesOrderDetail sod
            JOIN sod.product p
            JOIN sod.salesOrder so
            WHERE so.orderDate >= :startDate AND so.orderDate < :endDate
            GROUP BY p.productId, p.name
            ORDER BY SUM(sod.totalPrice) DESC
            """)
    List<Object[]> findRevenueGroupByProduct(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT u.name,
                   COALESCE(SUM(sod.totalPrice), 0),
                   COALESCE(SUM(sod.costPrice * sod.quantity), 0),
                   COUNT(DISTINCT so.salesOrderId)
            FROM SalesOrderDetail sod
            JOIN sod.salesOrder so
            JOIN so.cashier u
            WHERE so.orderDate >= :startDate AND so.orderDate < :endDate
            GROUP BY u.userId, u.name
            ORDER BY SUM(sod.totalPrice) DESC
            """)
    List<Object[]> findRevenueGroupByCashier(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);


    @Query("""
            SELECT p.productId, p.name, c.name, SUM(sod.quantity), SUM(sod.totalPrice), p.stockLevel, p.minStockLevel
            FROM SalesOrderDetail sod
            JOIN sod.product p
            LEFT JOIN p.category c
            JOIN sod.salesOrder so
            WHERE so.orderDate >= :startDate AND so.orderDate < :endDate
            GROUP BY p.productId, p.name, c.name, p.stockLevel, p.minStockLevel
            ORDER BY SUM(sod.quantity) DESC
            """)
    List<Object[]> findTopSellingProducts(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);


    @Query("""
            SELECT COALESCE(SUM(sod.quantity), 0)
            FROM SalesOrderDetail sod
            JOIN sod.salesOrder so
            WHERE sod.product.productId = :productId
              AND so.orderDate >= :startDate AND so.orderDate < :endDate
            """)
    Long countUnitsSoldByProduct(@Param("productId") Integer productId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);
}

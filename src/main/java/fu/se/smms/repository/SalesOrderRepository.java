package fu.se.smms.repository;

import fu.se.smms.entity.SalesOrder;
import fu.se.smms.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Integer> {

    @Query(value = """
            SELECT so FROM SalesOrder so
            LEFT JOIN FETCH so.cashier
            LEFT JOIN FETCH so.customer
            WHERE so.orderDate >= :startDate AND so.orderDate < :endDate
              AND (:cashierId IS NULL OR so.cashier.userId = :cashierId)
              AND (:paymentMethod IS NULL OR so.paymentMethod = :paymentMethod)
              AND (:customerId IS NULL OR so.customer.customerId = :customerId)
            """,
           countQuery = """
            SELECT COUNT(so) FROM SalesOrder so
            WHERE so.orderDate >= :startDate AND so.orderDate < :endDate
              AND (:cashierId IS NULL OR so.cashier.userId = :cashierId)
              AND (:paymentMethod IS NULL OR so.paymentMethod = :paymentMethod)
              AND (:customerId IS NULL OR so.customer.customerId = :customerId)
            """)
    Page<SalesOrder> search(@Param("startDate") LocalDateTime startDate,
                            @Param("endDate") LocalDateTime endDate,
                            @Param("cashierId") Integer cashierId,
                            @Param("paymentMethod") PaymentMethod paymentMethod,
                            @Param("customerId") Integer customerId,
                            Pageable pageable);

    @Query("SELECT COALESCE(SUM(so.totalAmount), 0) FROM SalesOrder so WHERE so.orderDate >= :startDate AND so.orderDate < :endDate")
    BigDecimal findTotalRevenue(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(so) FROM SalesOrder so WHERE so.orderDate >= :startDate AND so.orderDate < :endDate")
    Long countOrders(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);


    @Query("""
            SELECT so FROM SalesOrder so
            LEFT JOIN FETCH so.cashier
            LEFT JOIN FETCH so.customer
            LEFT JOIN FETCH so.salesOrderDetails
            ORDER BY so.orderDate DESC
            LIMIT 10
            """)
    List<SalesOrder> findTop10RecentOrders();


    @Query("SELECT MAX(so.invoiceNumber) FROM SalesOrder so WHERE so.invoiceNumber LIKE CONCAT(:prefix, '%')")
    Optional<String> findLastInvoiceNumberByPrefix(@Param("prefix") String prefix);


    @Query("""
            SELECT so FROM SalesOrder so
            LEFT JOIN FETCH so.cashier
            LEFT JOIN FETCH so.customer
            LEFT JOIN FETCH so.promotion
            LEFT JOIN FETCH so.salesOrderDetails sod
            LEFT JOIN FETCH sod.product p
            LEFT JOIN FETCH p.category
            WHERE so.salesOrderId = :id
            """)
    Optional<SalesOrder> findByIdWithDetails(@Param("id") Integer id);
}

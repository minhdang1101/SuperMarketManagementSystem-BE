package fu.se.smms.repository;

import fu.se.smms.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    @Query("""
            SELECT po FROM PurchaseOrder po
            JOIN FETCH po.supplier
            JOIN FETCH po.createdBy
            WHERE po.supplier.supplierId = :supplierId
            ORDER BY po.orderDate DESC
            """)
    List<PurchaseOrder> findBySupplierIdOrderByOrderDateDesc(@Param("supplierId") Integer supplierId);

    @Query("""
            SELECT po FROM PurchaseOrder po
            JOIN FETCH po.supplier
            JOIN FETCH po.createdBy
            WHERE (:supplierId IS NULL OR po.supplier.supplierId = :supplierId)
              AND (:status IS NULL OR po.status = :status)
            """)
    Page<PurchaseOrder> search(@Param("supplierId") Integer supplierId,
                               @Param("status") fu.se.smms.enums.OrderStatus status,
                               Pageable pageable);

    @Query("""
            SELECT po FROM PurchaseOrder po
            JOIN FETCH po.supplier
            JOIN FETCH po.createdBy
            LEFT JOIN FETCH po.purchaseOrderDetails pod
            LEFT JOIN FETCH pod.product
            WHERE po.poId = :id
            """)
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") Integer id);

    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.status = :status")
    Integer countByStatus(@Param("status") fu.se.smms.enums.OrderStatus status);
}

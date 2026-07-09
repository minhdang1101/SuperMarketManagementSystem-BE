package fu.se.smms.repository;

import fu.se.smms.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Integer> {

    @Query("""
            SELECT gr FROM GoodsReceipt gr
            JOIN FETCH gr.purchaseOrder
            JOIN FETCH gr.receivedBy
            LEFT JOIN FETCH gr.goodsReceiptDetails grd
            LEFT JOIN FETCH grd.product
            WHERE gr.receiptId = :id
            """)
    Optional<GoodsReceipt> findByIdWithDetails(@Param("id") Integer id);
}

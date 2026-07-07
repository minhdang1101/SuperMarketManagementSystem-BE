package fu.se.smms.entity;

import fu.se.smms.enums.GoodsReceiptStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Integer receiptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by", nullable = false)
    private User receivedBy;

    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "NVARCHAR(20)")
    private GoodsReceiptStatus status;

    @Column(name = "note", columnDefinition = "NVARCHAR(1000)")
    private String note;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<GoodsReceiptDetail> goodsReceiptDetails = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (receivedDate == null) receivedDate = LocalDateTime.now();
        if (status == null) status = GoodsReceiptStatus.PENDING;
    }
}

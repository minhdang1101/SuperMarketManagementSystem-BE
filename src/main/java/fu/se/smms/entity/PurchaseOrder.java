package fu.se.smms.entity;

import fu.se.smms.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id")
    private Integer poId;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "NVARCHAR(20)")
    private OrderStatus status;

    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "note", columnDefinition = "NVARCHAR(1000)")
    private String note;

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<PurchaseOrderDetail> purchaseOrderDetails = new ArrayList<>();

    @OneToMany(mappedBy = "purchaseOrder")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<GoodsReceipt> goodsReceipts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) orderDate = LocalDateTime.now();
        if (status == null) status = OrderStatus.DRAFT;
    }
}

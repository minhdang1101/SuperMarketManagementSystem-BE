package fu.se.smms.entity;

import fu.se.smms.enums.PaymentMethod;
import fu.se.smms.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_order", indexes = {
        @Index(name = "idx_so_order_date", columnList = "order_date"),
        @Index(name = "idx_so_cashier_id", columnList = "cashier_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_id")
    private Integer salesOrderId;

    @Column(name = "invoice_number", nullable = false, unique = true, columnDefinition = "NVARCHAR(50)")
    private String invoiceNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, columnDefinition = "NVARCHAR(20)")
    private PaymentMethod paymentMethod;

    @Column(name = "received_amount", precision = 19, scale = 4)
    private BigDecimal receivedAmount;

    @Column(name = "change_amount", precision = 19, scale = 4)
    private BigDecimal changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, columnDefinition = "NVARCHAR(20)")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "vnpay_transaction_no", columnDefinition = "NVARCHAR(100)")
    private String vnpayTransactionNo;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<SalesOrderDetail> salesOrderDetails = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) orderDate = LocalDateTime.now();
    }
}

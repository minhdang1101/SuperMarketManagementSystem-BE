package fu.se.smms.entity;

import fu.se.smms.enums.AdjustmentReason;
import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_adjustment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adjustment_id")
    private Integer adjustmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjusted_by", nullable = false)
    private User adjustedBy;

    @Positive
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, columnDefinition = "NVARCHAR(20)")
    private AdjustmentReason reason;

    @Column(name = "note", columnDefinition = "NVARCHAR(1000)")
    private String note;

    @Column(name = "adjusted_at", nullable = false)
    private LocalDateTime adjustedAt;

    @PrePersist
    protected void onCreate() {
        if (adjustedAt == null) adjustedAt = LocalDateTime.now();
    }
}

package fu.se.smms.entity;

import fu.se.smms.enums.ApplyTarget;
import fu.se.smms.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotion", indexes = {
        @Index(name = "idx_promo_start_date", columnList = "start_date"),
        @Index(name = "idx_promo_end_date", columnList = "end_date"),
        @Index(name = "idx_promo_discount_type", columnList = "discount_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Promotion extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Integer promotionId;

    @Column(name = "promo_code", nullable = true, unique = true, columnDefinition = "NVARCHAR(20)")
    private String promoCode;

    @Column(name = "name", nullable = false, columnDefinition = "NVARCHAR(200)")
    private String name;

    @Column(name = "description", columnDefinition = "NVARCHAR(1000)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, columnDefinition = "NVARCHAR(20)")
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_target", nullable = true, columnDefinition = "NVARCHAR(20)")
    private ApplyTarget applyTarget;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "get_quantity")
    private Integer getQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "get_product_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product getProduct;

    @Column(name = "min_order_amount", precision = 19, scale = 4)
    private BigDecimal minOrderAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "promotion_product",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @Transient
    public boolean isCurrentlyActive() {
        LocalDate today = LocalDate.now();
        return Boolean.TRUE.equals(active)
                && !today.isBefore(startDate)
                && !today.isAfter(endDate);
    }
}

package fu.se.smms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_check_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheckDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "icd_id")
    private Integer icdId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_id", nullable = false)
    private InventoryCheck inventoryCheck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity;

    @Column(name = "difference", nullable = false)
    private Integer difference;

    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;
}

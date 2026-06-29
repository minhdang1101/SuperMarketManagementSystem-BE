package fu.se.smms.entity;

import fu.se.smms.enums.InventoryCheckStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_check")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id")
    private Integer checkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by", nullable = false)
    private User checkedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "check_date", nullable = false)
    private LocalDateTime checkDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "NVARCHAR(20)")
    private InventoryCheckStatus status;

    @Column(name = "note", columnDefinition = "NVARCHAR(1000)")
    private String note;

    @OneToMany(mappedBy = "inventoryCheck", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<InventoryCheckDetail> inventoryCheckDetails = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (checkDate == null) checkDate = LocalDateTime.now();
        if (status == null) status = InventoryCheckStatus.IN_PROGRESS;
    }
}

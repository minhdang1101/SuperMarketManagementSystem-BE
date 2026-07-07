package fu.se.smms.dto;

import fu.se.smms.enums.AdjustmentReason;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentResponseDTO {
    private Integer adjustmentId;
    private Integer productId;
    private String productName;
    private String barcode;
    private Integer quantity;
    private AdjustmentReason reason;
    private String note;
    private LocalDateTime adjustedAt;
    private Integer adjustedByUserId;
    private String adjustedByName;
    private Integer stockAfterAdjustment;
}

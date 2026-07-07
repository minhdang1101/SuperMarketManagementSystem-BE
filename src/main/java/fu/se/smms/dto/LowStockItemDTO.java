package fu.se.smms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowStockItemDTO {
    private Integer productId;
    private String productName;
    private String categoryName;
    private Integer currentStock;
    private Integer reorderLevel;
    private Integer shortage;
}

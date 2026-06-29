package fu.se.smms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementItemDTO {
    private Integer productId;
    private String productName;
    private String categoryName;
    private Integer stockIn;
    private Integer stockOut;
    private Integer adjustments;
    private Integer netChange;
}

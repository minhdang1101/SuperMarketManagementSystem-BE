package fu.se.smms.dto;

import fu.se.smms.enums.StockStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryStockDTO {
    private Integer productId;
    private String productName;
    private String barcode;
    private String categoryName;
    private Integer totalImported;
    private Integer totalExported;
    private Integer totalAdjusted;
    private Integer currentStock;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private StockStatus stockStatus;
}

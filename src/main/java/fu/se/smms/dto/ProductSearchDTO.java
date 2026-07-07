package fu.se.smms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDTO {
    
    private Integer productId;
    private String barcode;
    private String name;
    private String categoryName;
    private BigDecimal sellingPrice;
    private Integer stockLevel;
    private String imageUrl;

    private Boolean hasPromotion;

    private String promotionBadge;
}

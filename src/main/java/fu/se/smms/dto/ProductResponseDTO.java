package fu.se.smms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private Integer productId;
    private String name;
    private String barcode;
    private String description;
    private String unit;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private Integer stockLevel;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private List<String> images;
    private Integer categoryId;
    private String categoryName;
    private Integer supplierId;
    private String supplierName;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

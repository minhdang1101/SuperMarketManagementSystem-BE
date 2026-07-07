package fu.se.smms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.se.smms.enums.ApplyTarget;
import fu.se.smms.enums.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionResponseDTO {

    private Integer promotionId;
    private String promoCode;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private ApplyTarget applyTarget;

    private Integer categoryId;
    private String categoryName;

    private List<Integer> productIds;
    private List<String> productNames;

    private Integer buyQuantity;
    private Integer getQuantity;
    private Integer getProductId;
    private String getProductName;

    private BigDecimal minOrderAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate validFrom;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate validTo;

    private Boolean active;
    private Boolean currentlyActive;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime createdAt;
    private String createdBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime updatedAt;
    private String lastModifiedBy;
}

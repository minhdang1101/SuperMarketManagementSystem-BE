package fu.se.smms.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderDetailResponseDTO {

    private Integer sodId;
    private Integer productId;
    private String productName;
    private String barcode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal costPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
}

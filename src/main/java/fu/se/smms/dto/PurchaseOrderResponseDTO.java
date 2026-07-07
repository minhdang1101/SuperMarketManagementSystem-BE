package fu.se.smms.dto;

import fu.se.smms.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponseDTO {
    private Integer poId;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String note;
    private LocalDateTime expectedDeliveryDate;
    private Integer supplierId;
    private String supplierName;
    private Integer createdByUserId;
    private String createdByName;
    private List<PurchaseOrderDetailResponseDTO> details;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseOrderDetailResponseDTO {
        private Integer podId;
        private Integer productId;
        private String productName;
        private String barcode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}

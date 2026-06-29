package fu.se.smms.dto;

import fu.se.smms.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Checkout Response DTO - POS checkout response with order details
 * 
 * @author SMS Development Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponseDTO {

    // Order info (populated after complete)
    private Integer orderId;
    private String invoiceNumber;
    private LocalDateTime orderDate;
    private String cashierName;

    // Customer info
    private String customerName;
    private String memberCardId;
    private Integer loyaltyPoints;

    // Items
    private List<CheckoutItemDTO> items;

    // Totals
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private String discountDescription;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    // Payment
    private PaymentMethod paymentMethod;
    private BigDecimal receivedAmount;
    private BigDecimal changeAmount;

    // Promotion applied
    private String promotionCode;
    private String promotionName;

    /**
     * Checkout Item DTO - Single line item in checkout
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutItemDTO {
        private Integer productId;
        private String barcode;
        private String productName;
        private String categoryName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;
        private BigDecimal discountAmount;
        private String imageUrl;
    }
}

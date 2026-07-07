package fu.se.smms.dto;

import fu.se.smms.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDTO {

    @NotEmpty(message = "Giỏ hàng không được trống")
    @Valid
    private List<CartItem> items;


    @Size(max = 50, message = "Mã thẻ thành viên không được quá 50 ký tự")
    private String memberCardId;

    @Size(max = 20, message = "Mã khuyến mãi không được quá 20 ký tự")
    private String promoCode;

    @NotNull(message = "Phương thức thanh toán là bắt buộc")
    private PaymentMethod paymentMethod;

    @DecimalMin(value = "0", message = "Số tiền nhận phải >= 0")
    private BigDecimal receivedAmount;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {
        
        @NotNull(message = "Product ID là bắt buộc")
        @Positive(message = "Product ID phải > 0")
        private Integer productId;

        @NotNull(message = "Số lượng là bắt buộc")
        @Min(value = 1, message = "Số lượng phải >= 1")
        @Max(value = 999, message = "Số lượng không được quá 999")
        private Integer quantity;
    }
}

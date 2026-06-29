package fu.se.smms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.se.smms.enums.ApplyTarget;
import fu.se.smms.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionCreateReqDTO {

    @NotBlank(message = "Tên chương trình khuyến mãi không được để trống")
    @Size(max = 200, message = "Tên tối đa 200 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    @NotNull(message = "Loại khuyến mãi không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    @Digits(integer = 15, fraction = 4, message = "Giá trị giảm không hợp lệ")
    private BigDecimal discountValue;

    @NotNull(message = "Đối tượng áp dụng không được để trống")
    private ApplyTarget applyTarget;

    private Integer categoryId;

    private List<Integer> productIds;

    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    private Integer buyQuantity;

    @Min(value = 1, message = "Số lượng tặng tối thiểu là 1")
    private Integer getQuantity;

    private Integer getProductId;

    @DecimalMin(value = "0", message = "Giá trị đơn hàng tối thiểu không được âm")
    @Digits(integer = 15, fraction = 4, message = "Giá trị đơn hàng tối thiểu không hợp lệ")
    private BigDecimal minOrderAmount;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @FutureOrPresent(message = "BR-04: Ngày bắt đầu không được nằm trong quá khứ")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate validFrom;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate validTo;
}

package fu.se.smms.dto;

import fu.se.smms.enums.ApplyTarget;
import fu.se.smms.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionUpdateReqDTO {

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    @Size(max = 100, message = "Tên khuyến mãi không được quá 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String description;

    @NotNull(message = "Loại giảm giá là bắt buộc")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm là bắt buộc")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải > 0")
    private BigDecimal discountValue;

    @NotNull(message = "Đối tượng áp dụng là bắt buộc")
    private ApplyTarget applyTarget;

    private Integer categoryId;

    private List<Integer> productIds;

    @Min(value = 1, message = "Số lượng mua phải >= 1")
    private Integer buyQuantity;

    @Min(value = 1, message = "Số lượng tặng phải >= 1")
    private Integer getQuantity;

    private Integer getProductId;

    @DecimalMin(value = "0", message = "Giá trị đơn tối thiểu phải >= 0")
    private BigDecimal minOrderAmount;

    @NotNull(message = "Ngày bắt đầu là bắt buộc")
    private LocalDate validFrom;

    @NotNull(message = "Ngày kết thúc là bắt buộc")
    private LocalDate validTo;

    private Boolean active;
}

package fu.se.smms.dto;

import fu.se.smms.enums.AdjustmentReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentDTO {
    @NotNull(message = "Product ID không được để trống")
    @Positive(message = "Product ID phải là số dương")
    private Integer productId;

    @NotNull(message = "Số lượng trừ không được để trống")
    @Positive(message = "Số lượng trừ phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Lý do không được để trống")
    private AdjustmentReason reason;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String note;
}

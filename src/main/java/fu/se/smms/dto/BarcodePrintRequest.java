package fu.se.smms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarcodePrintRequest {

    @NotEmpty(message = "Danh sách sản phẩm không được trống")
    @Valid
    private List<PrintItem> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrintItem {
        @NotNull(message = "ID sản phẩm không được để trống")
        @Positive(message = "ID sản phẩm phải là số dương")
        private Integer productId;

        @NotNull(message = "Số lượng nhãn không được để trống")
        @Min(value = 1, message = "Số lượng nhãn phải >= 1")
        private Integer quantity;
    }
}

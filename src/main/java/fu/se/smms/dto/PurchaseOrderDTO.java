package fu.se.smms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDTO {
    @NotNull(message = "Supplier ID không được để trống")
    @Positive(message = "Supplier ID phải là số dương")
    private Integer supplierId;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String note;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expectedDeliveryDate;

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<PurchaseOrderItemDTO> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PurchaseOrderItemDTO {
        @NotNull(message = "Product ID không được để trống")
        @Positive(message = "Product ID phải là số dương")
        private Integer productId;

        @NotNull(message = "Số lượng không được để trống")
        @Positive(message = "Số lượng phải lớn hơn 0")
        private Integer quantity;
    }
}

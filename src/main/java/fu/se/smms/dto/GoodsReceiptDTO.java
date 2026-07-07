package fu.se.smms.dto;

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
public class GoodsReceiptDTO {
    @NotNull(message = "PO ID không được để trống")
    @Positive(message = "PO ID phải là số dương")
    private Integer poId;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String note;

    @NotEmpty(message = "Danh sách sản phẩm nhận không được để trống")
    @Valid
    private List<GoodsReceiptItemDTO> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoodsReceiptItemDTO {
        @NotNull(message = "Product ID không được để trống")
        @Positive(message = "Product ID phải là số dương")
        private Integer productId;

        @NotNull(message = "Số lượng nhận thực tế không được để trống")
        @Positive(message = "Số lượng nhận phải lớn hơn 0")
        private Integer receivedQuantity;

        private LocalDate expiryDate;

        private String batchNumber;
    }
}

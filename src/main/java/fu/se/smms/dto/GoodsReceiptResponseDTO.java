package fu.se.smms.dto;

import fu.se.smms.enums.GoodsReceiptStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptResponseDTO {
    private Integer receiptId;
    private Integer poId;
    private LocalDateTime receivedDate;
    private GoodsReceiptStatus status;
    private String note;
    private Integer receivedByUserId;
    private String receivedByName;
    private List<GoodsReceiptDetailResponseDTO> details;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoodsReceiptDetailResponseDTO {
        private Integer grdId;
        private Integer productId;
        private String productName;
        private String barcode;
        private Integer orderedQuantity;
        private Integer receivedQuantity;
        private LocalDate expiryDate;
        private String batchNumber;
    }
}

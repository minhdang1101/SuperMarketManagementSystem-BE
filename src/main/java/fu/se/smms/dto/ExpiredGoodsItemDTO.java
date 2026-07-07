package fu.se.smms.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpiredGoodsItemDTO {
    private Integer productId;
    private String productName;
    private String batchNumber;
    private LocalDate expiryDate;
    private Integer quantity;
    private String status;
}

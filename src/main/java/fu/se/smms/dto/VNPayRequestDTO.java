package fu.se.smms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayRequestDTO {
    private Integer orderId;
    private BigDecimal amount;
    private String orderInfo;
    private String bankCode;
    private String language;
}

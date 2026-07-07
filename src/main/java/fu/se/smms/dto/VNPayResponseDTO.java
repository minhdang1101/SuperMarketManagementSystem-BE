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
public class VNPayResponseDTO {
    private String code;
    private String message;
    private String paymentUrl;
    
    // Transaction details (for return/IPN)
    private String transactionNo;
    private String txnRef;
    private BigDecimal amount;
    private String orderInfo;
    private String responseCode;
    private String transactionStatus;
    private String bankCode;
    private String bankTranNo;
    private String payDate;
    
    public static VNPayResponseDTO success(String paymentUrl) {
        return VNPayResponseDTO.builder()
                .code("00")
                .message("Thành công")
                .paymentUrl(paymentUrl)
                .build();
    }
    
    public static VNPayResponseDTO error(String code, String message) {
        return VNPayResponseDTO.builder()
                .code(code)
                .message(message)
                .build();
    }
}

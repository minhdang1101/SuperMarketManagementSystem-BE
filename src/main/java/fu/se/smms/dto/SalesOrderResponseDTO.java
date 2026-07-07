package fu.se.smms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fu.se.smms.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderResponseDTO {

    private Integer salesOrderId;
    private String invoiceNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime orderDate;

    private Integer cashierId;
    private String cashierName;
    private Integer customerId;
    private String customerName;
    private PaymentMethod paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal changeAmount;

    private List<SalesOrderDetailResponseDTO> details;
}

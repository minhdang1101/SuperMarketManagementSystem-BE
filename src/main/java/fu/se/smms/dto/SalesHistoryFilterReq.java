package fu.se.smms.dto;

import fu.se.smms.enums.PaymentMethod;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesHistoryFilterReq {

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Integer cashierId;
    private PaymentMethod paymentMethod;
    private Integer customerId;
}

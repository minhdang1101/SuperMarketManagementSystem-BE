package fu.se.smms.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDetailResDTO {

    private String groupName;
    private BigDecimal revenue;
    private BigDecimal cogs;
    private BigDecimal grossProfit;
    private BigDecimal profitMargin;
    private Long orderCount;
}

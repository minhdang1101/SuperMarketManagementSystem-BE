package fu.se.smms.dto;

import fu.se.smms.enums.ReportGroupBy;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportFilterReq {

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private ReportGroupBy groupBy;
}

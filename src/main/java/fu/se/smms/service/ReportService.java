package fu.se.smms.service;

import fu.se.smms.dto.ReportFilterReq;
import fu.se.smms.dto.ReportSummaryResDTO;

public interface ReportService {

    ReportSummaryResDTO getRevenueReport(ReportFilterReq filter);
}

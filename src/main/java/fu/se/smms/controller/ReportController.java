package fu.se.smms.controller;

import fu.se.smms.dto.ReportFilterReq;
import fu.se.smms.dto.ReportSummaryResDTO;
import fu.se.smms.enums.ReportGroupBy;
import fu.se.smms.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/api/v1/reports")
@Tag(name = "Revenue & Profit Reports", description = "Báo cáo doanh thu và lợi nhuận. Chỉ dành cho ADMIN và MANAGER.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/revenue")
    @Operation(summary = "Báo cáo doanh thu & lợi nhuận (tối đa 365 ngày, group by Category/Product/Cashier)")
    public ResponseEntity<ReportSummaryResDTO> getRevenueReport(
            @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dateFrom,
            @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dateTo,
            @RequestParam(required = false) ReportGroupBy groupBy) {

        log.info("GET /api/v1/reports/revenue - dateFrom={}, dateTo={}, groupBy={}", dateFrom, dateTo, groupBy);

        ReportFilterReq filter = ReportFilterReq.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .groupBy(groupBy)
                .build();

        return ResponseEntity.ok(reportService.getRevenueReport(filter));
    }
}

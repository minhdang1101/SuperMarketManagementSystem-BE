package fu.se.smms.controller;

import fu.se.smms.dto.DashboardSummaryDTO;
import fu.se.smms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Dashboard summary statistics and analytics")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Lấy thống kê tổng quan dashboard",
        description = "Trả về thống kê doanh thu, đơn hàng, cảnh báo tồn kho, xu hướng bán hàng và sản phẩm bán chạy"
    )
    public ResponseEntity<DashboardSummaryDTO> getSummary(
            @Parameter(description = "Kỳ báo cáo: today (hôm nay), week (tuần này), month (tháng này)")
            @RequestParam(defaultValue = "today") String period) {
        
        log.info("GET /api/v1/dashboard/summary - period={}", period);
        
        try {
            DashboardSummaryDTO summary = dashboardService.getSummary(period);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid period parameter: {}", period);
            throw e;
        }
    }
}

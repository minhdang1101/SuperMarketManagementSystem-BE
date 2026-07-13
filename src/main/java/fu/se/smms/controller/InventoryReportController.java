package fu.se.smms.controller;

import fu.se.smms.dto.ExpiredGoodsItemDTO;
import fu.se.smms.dto.LowStockItemDTO;
import fu.se.smms.dto.StockMovementItemDTO;
import fu.se.smms.service.InventoryReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/reports/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class InventoryReportController {
    private static final Logger log = LoggerFactory.getLogger(InventoryReportController.class);
    private final InventoryReportService inventoryReportService;

    public InventoryReportController(InventoryReportService inventoryReportService) {
        this.inventoryReportService = inventoryReportService;
    }

    @GetMapping("/stock-movement")
    public ResponseEntity<List<StockMovementItemDTO>> getStockMovementReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) Integer categoryId) {
        log.info("Stock Movement report: {} to {}, categoryId={}", dateFrom, dateTo, categoryId);
        return ResponseEntity.ok(inventoryReportService.getStockMovementReport(dateFrom, dateTo, categoryId));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockItemDTO>> getLowStockReport() {
        log.info("Low Stock report requested");
        return ResponseEntity.ok(inventoryReportService.getLowStockReport());
    }

    @GetMapping("/expired-goods")
    public ResponseEntity<List<ExpiredGoodsItemDTO>> getExpiredGoodsReport() {
        log.info("Expired Goods report requested");
        return ResponseEntity.ok(inventoryReportService.getExpiredGoodsReport());
    }
}

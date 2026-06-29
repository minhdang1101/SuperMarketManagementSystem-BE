package fu.se.smms.controller;

import fu.se.smms.dto.StockAdjustmentDTO;
import fu.se.smms.dto.StockAdjustmentResponseDTO;
import fu.se.smms.service.StockService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/stock")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class StockController {
    private static final Logger log = LoggerFactory.getLogger(StockController.class);
    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockAdjustmentResponseDTO> adjustStock(
            @Valid @RequestBody StockAdjustmentDTO dto) {
        log.info("Stock adjustment: productId={}, quantity={}, reason={}",
                dto.getProductId(), dto.getQuantity(), dto.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.adjustStock(dto));
    }

    @GetMapping("/adjustments")
    public ResponseEntity<Page<StockAdjustmentResponseDTO>> getAdjustmentHistory(
            @RequestParam(required = false) Integer productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "adjustmentId") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        log.debug("Get adjustment history: productId={}, page={}, size={}", productId, page, size);
        return ResponseEntity.ok(stockService.getAdjustmentHistory(productId, pageable));
    }
}

package fu.se.smms.controller;

import fu.se.smms.dto.InventoryStockDTO;
import fu.se.smms.service.InventoryService;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class InventoryController {
    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stock")
    public ResponseEntity<List<InventoryStockDTO>> getAllStockInfo() {
        log.debug("Get all stock info");
        return ResponseEntity.ok(inventoryService.getAllStockInfo());
    }

    @GetMapping("/stock/{productId}")
    public ResponseEntity<InventoryStockDTO> getStockInfo(@PathVariable @Positive Integer productId) {
        log.debug("Get stock info for product id: {}", productId);
        return ResponseEntity.ok(inventoryService.getStockInfo(productId));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryStockDTO>> getLowStockProducts() {
        log.debug("Get low stock products");
        return ResponseEntity.ok(inventoryService.getLowStockProducts());
    }
}

package fu.se.smms.controller;

import fu.se.smms.dto.PurchaseOrderDTO;
import fu.se.smms.dto.PurchaseOrderResponseDTO;
import fu.se.smms.dto.PurchaseOrderStatusDTO;
import fu.se.smms.enums.OrderStatus;
import fu.se.smms.service.PurchaseOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {
    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderController.class);
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    public ResponseEntity<Page<PurchaseOrderResponseDTO>> search(
            @RequestParam(required = false) Integer supplierId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "poId") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        log.debug("Search purchase orders: supplierId={}, status={}, page={}, size={}", supplierId, status, page, size);
        return ResponseEntity.ok(purchaseOrderService.search(supplierId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDTO> findById(@PathVariable @Positive Integer id) {
        log.debug("Find purchase order by id: {}", id);
        return ResponseEntity.ok(purchaseOrderService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponseDTO> create(@Valid @RequestBody PurchaseOrderDTO dto) {
        log.info("Create purchase order for supplier id: {}", dto.getSupplierId());
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.create(dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PurchaseOrderResponseDTO> updateStatus(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody PurchaseOrderStatusDTO statusDTO) {
        log.info("Update PO id: {} status to {}", id, statusDTO.getStatus());
        return ResponseEntity.ok(purchaseOrderService.updateStatus(id, statusDTO.getStatus()));
    }
}

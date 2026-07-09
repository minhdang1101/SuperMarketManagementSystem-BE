package fu.se.smms.controller;

import fu.se.smms.dto.SupplierDTO;
import fu.se.smms.dto.SupplierResponseDTO;
import fu.se.smms.service.SupplierService;
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
@RequestMapping("/api/v1/suppliers")
public class SupplierController {
    private static final Logger log = LoggerFactory.getLogger(SupplierController.class);
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<Page<SupplierResponseDTO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "supplierId") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        log.debug("Search suppliers: keyword={}, status={}, page={}, size={}", keyword, status, page, size);
        return ResponseEntity.ok(supplierService.search(keyword, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> findById(@PathVariable @Positive Integer id) {
        log.debug("Find supplier by id: {}", id);
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<SupplierResponseDTO> findByIdWithPurchaseOrders(@PathVariable @Positive Integer id) {
        log.debug("Find supplier details with purchase orders: {}", id);
        return ResponseEntity.ok(supplierService.findByIdWithPurchaseOrders(id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponseDTO> create(@Valid @RequestBody SupplierDTO supplierDTO) {
        log.info("Create supplier: {}", supplierDTO.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(supplierDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDTO> update(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody SupplierDTO supplierDTO) {
        log.info("Update supplier id: {}", id);
        return ResponseEntity.ok(supplierService.update(id, supplierDTO));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<SupplierResponseDTO> toggleStatus(@PathVariable @Positive Integer id) {
        log.info("Toggle status for supplier id: {}", id);
        return ResponseEntity.ok(supplierService.toggleStatus(id));
    }
}

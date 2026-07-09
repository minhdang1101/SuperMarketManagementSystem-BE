package fu.se.smms.controller;

import fu.se.smms.dto.PromotionCreateReqDTO;
import fu.se.smms.dto.PromotionResponseDTO;
import fu.se.smms.dto.PromotionUpdateReqDTO;
import fu.se.smms.enums.ApplyTarget;
import fu.se.smms.enums.DiscountType;
import fu.se.smms.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/promotions")
@Tag(name = "Promotion Management", description = "Quản lý khuyến mãi. Yêu cầu quyền ADMIN hoặc MANAGER.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class PromotionController {

    private static final Logger log = LoggerFactory.getLogger(PromotionController.class);
    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    @Operation(summary = "Tạo mới chương trình khuyến mãi")
    public ResponseEntity<PromotionResponseDTO> create(@Valid @RequestBody PromotionCreateReqDTO dto) {
        log.info("POST /api/v1/promotions - Tạo promotion: name={}, type={}", dto.getName(), dto.getDiscountType());
        PromotionResponseDTO response = promotionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách khuyến mãi (phân trang + filter)")
    public ResponseEntity<Page<PromotionResponseDTO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) DiscountType discountType,
            @RequestParam(required = false) ApplyTarget applyTarget,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "promotionId") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        log.debug("GET /api/v1/promotions - keyword={}, type={}, target={}, active={}, page={}, size={}",
                keyword, discountType, applyTarget, active, page, size);
        return ResponseEntity.ok(promotionService.search(keyword, discountType, applyTarget, active, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết khuyến mãi theo ID")
    public ResponseEntity<PromotionResponseDTO> findById(@PathVariable @Positive Integer id) {
        log.debug("GET /api/v1/promotions/{} - Lấy chi tiết", id);
        return ResponseEntity.ok(promotionService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật chương trình khuyến mãi")
    public ResponseEntity<PromotionResponseDTO> update(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody PromotionUpdateReqDTO dto) {
        log.info("PUT /api/v1/promotions/{} - Cập nhật promotion: name={}", id, dto.getName());
        PromotionResponseDTO response = promotionService.update(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa chương trình khuyến mãi")
    public ResponseEntity<Void> delete(@PathVariable @Positive Integer id) {
        log.info("DELETE /api/v1/promotions/{} - Xóa promotion", id);
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

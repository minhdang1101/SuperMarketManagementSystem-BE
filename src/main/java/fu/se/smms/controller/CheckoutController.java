package fu.se.smms.controller;

import fu.se.smms.dto.CheckoutRequestDTO;
import fu.se.smms.dto.CheckoutResponseDTO;
import fu.se.smms.dto.ProductSearchDTO;
import fu.se.smms.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@Validated
@RequestMapping("/api/v1/pos")
@Tag(name = "POS Checkout", description = "Point of Sale operations - Scan, calculate, and checkout")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @GetMapping("/products")
    @Operation(
        summary = "Tìm kiếm sản phẩm cho POS",
        description = "Tìm sản phẩm theo tên hoặc mã vạch. Chỉ trả về sản phẩm đang active."
    )
    public ResponseEntity<Page<ProductSearchDTO>> searchProducts(
            @Parameter(description = "Từ khóa tìm kiếm (tên hoặc mã vạch)")
            @RequestParam(required = false) String query,
            @Parameter(description = "ID danh mục để lọc")
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("GET /api/v1/pos/products - query={}, categoryId={}", query, categoryId);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(checkoutService.searchProducts(query, categoryId, pageable));
    }

    @GetMapping("/products/barcode/{barcode}")
    @Operation(
        summary = "Lấy sản phẩm theo mã vạch",
        description = "Dùng cho máy quét mã vạch"
    )
    public ResponseEntity<ProductSearchDTO> getProductByBarcode(
            @PathVariable @NotBlank String barcode) {
        log.debug("GET /api/v1/pos/products/barcode/{}", barcode);
        return ResponseEntity.ok(checkoutService.getProductByBarcode(barcode));
    }

    @PostMapping("/calculate")
    @Operation(
        summary = "Tính tổng giỏ hàng",
        description = "Tính tổng với khuyến mãi. Không tạo đơn hàng, không trừ tồn kho."
    )
    public ResponseEntity<CheckoutResponseDTO> calculateTotal(
            @Valid @RequestBody CheckoutRequestDTO request) {
        log.debug("POST /api/v1/pos/calculate - items={}", request.getItems().size());
        return ResponseEntity.ok(checkoutService.calculateTotal(request));
    }


    @PostMapping("/checkout")
    @Operation(
        summary = "Hoàn tất thanh toán",
        description = "Tạo đơn hàng, trừ tồn kho, cộng điểm khách hàng. Trả về hóa đơn để in."
    )
    public ResponseEntity<CheckoutResponseDTO> completeCheckout(
            @Valid @RequestBody CheckoutRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String username = userDetails.getUsername();
        log.info("POST /api/v1/pos/checkout - cashier={}, items={}", username, request.getItems().size());
        
        CheckoutResponseDTO response = checkoutService.completeCheckout(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

package fu.se.smms.controller;

import fu.se.smms.dto.SalesHistoryFilterReq;
import fu.se.smms.dto.SalesOrderResponseDTO;
import fu.se.smms.enums.PaymentMethod;
import fu.se.smms.service.SalesHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/api/v1/sales")
@Tag(name = "Sales History", description = "Xem lịch sử bán hàng. ADMIN và CASHIER xem toàn bộ, MANAGER chỉ xem hóa đơn mình tạo.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
public class SalesController {

    private static final Logger log = LoggerFactory.getLogger(SalesController.class);
    private final SalesHistoryService salesHistoryService;

    public SalesController(SalesHistoryService salesHistoryService) {
        this.salesHistoryService = salesHistoryService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách hóa đơn (mặc định hôm nay, tối đa 90 ngày)")
    public ResponseEntity<Page<SalesOrderResponseDTO>> search(
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate dateTo,
            @RequestParam(required = false) Integer cashierId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        SalesHistoryFilterReq filter = SalesHistoryFilterReq.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .cashierId(cashierId)
                .paymentMethod(paymentMethod)
                .customerId(customerId)
                .build();

        log.debug("GET /api/v1/sales - dateFrom={}, dateTo={}, cashierId={}, paymentMethod={}, customerId={}",
                dateFrom, dateTo, cashierId, paymentMethod, customerId);
        return ResponseEntity.ok(salesHistoryService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết hóa đơn bao gồm danh sách sản phẩm")
    public ResponseEntity<SalesOrderResponseDTO> findById(@PathVariable @Positive Integer id) {
        log.debug("GET /api/v1/sales/{}", id);
        return ResponseEntity.ok(salesHistoryService.findById(id));
    }
}

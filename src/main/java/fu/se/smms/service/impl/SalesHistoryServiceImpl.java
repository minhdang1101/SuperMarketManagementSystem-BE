package fu.se.smms.service.impl;

import fu.se.smms.dto.SalesHistoryFilterReq;
import fu.se.smms.dto.SalesOrderDetailResponseDTO;
import fu.se.smms.dto.SalesOrderResponseDTO;
import fu.se.smms.entity.SalesOrder;
import fu.se.smms.entity.SalesOrderDetail;
import fu.se.smms.exception.BusinessRuleException;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.SalesOrderDetailRepository;
import fu.se.smms.repository.SalesOrderRepository;
import fu.se.smms.repository.UserRepository;
import fu.se.smms.service.SalesHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SalesHistoryServiceImpl implements SalesHistoryService {

    private static final Logger log = LoggerFactory.getLogger(SalesHistoryServiceImpl.class);
    private static final long MAX_DATE_RANGE_DAYS = 90;

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderDetailRepository salesOrderDetailRepository;
    private final UserRepository userRepository;

    public SalesHistoryServiceImpl(SalesOrderRepository salesOrderRepository,
                                   SalesOrderDetailRepository salesOrderDetailRepository,
                                   UserRepository userRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderDetailRepository = salesOrderDetailRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<SalesOrderResponseDTO> search(SalesHistoryFilterReq filter, Pageable pageable) {
        LocalDate dateFrom = filter.getDateFrom() != null ? filter.getDateFrom() : LocalDate.now();
        LocalDate dateTo = filter.getDateTo() != null ? filter.getDateTo() : LocalDate.now();

        validateDateRange(dateFrom, dateTo);

        Integer cashierId = resolveCashierId(filter.getCashierId());

        LocalDateTime startDate = dateFrom.atStartOfDay();
        LocalDateTime endDate = dateTo.plusDays(1).atStartOfDay();

        log.debug("Search sales history: dateFrom={}, dateTo={}, cashierId={}, paymentMethod={}, customerId={}",
                dateFrom, dateTo, cashierId, filter.getPaymentMethod(), filter.getCustomerId());
 
        return salesOrderRepository.search(startDate, endDate, cashierId, filter.getPaymentMethod(), filter.getCustomerId(), pageable)
                .map(this::toResponseDTO);
    }

    @Override
    public SalesOrderResponseDTO findById(Integer id) {
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MSG01 - Không tìm thấy hóa đơn với id: " + id));

        enforceAccessControl(order);

        SalesOrderResponseDTO dto = toResponseDTO(order);
        List<SalesOrderDetail> details = salesOrderDetailRepository.findBySalesOrder_SalesOrderId(id);
        dto.setDetails(details.stream().map(this::toDetailDTO).toList());
        return dto;
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateTo.isBefore(dateFrom)) {
            throw new BusinessRuleException("MSG03", "Ngày kết thúc phải >= ngày bắt đầu");
        }
        long daysBetween = ChronoUnit.DAYS.between(dateFrom, dateTo);
        if (daysBetween > MAX_DATE_RANGE_DAYS) {
            throw new BusinessRuleException("MSG03",
                    "BR-02: Khoảng cách ngày không được vượt quá " + MAX_DATE_RANGE_DAYS
                            + " ngày. Hiện tại: " + daysBetween + " ngày");
        }
    }

    private Integer resolveCashierId(Integer requestedCashierId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isCashier = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CASHIER".equals(a.getAuthority()));

        // ADMIN và CASHIER có thể xem tất cả đơn hàng
        if (isAdmin || isCashier) {
            return requestedCashierId;
        }

        // MANAGER chỉ xem đơn hàng của mình
        String username = auth.getName();
        Integer currentUserId = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username))
                .getUserId();
        return currentUserId;
    }

    private void enforceAccessControl(SalesOrder order) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isCashier = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CASHIER".equals(a.getAuthority()));

        // ADMIN và CASHIER có thể xem tất cả đơn hàng
        if (isAdmin || isCashier) {
            return;
        }

        // MANAGER chỉ xem đơn hàng của mình
        String username = auth.getName();
        Integer currentUserId = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username))
                .getUserId();
        if (!currentUserId.equals(order.getCashier().getUserId())) {
            throw new BusinessRuleException("MSG07", "Bạn không có quyền xem hóa đơn này");
        }
    }

    private SalesOrderResponseDTO toResponseDTO(SalesOrder so) {
        return SalesOrderResponseDTO.builder()
                .salesOrderId(so.getSalesOrderId())
                .invoiceNumber(so.getInvoiceNumber())
                .orderDate(so.getOrderDate())
                .cashierId(so.getCashier() != null ? so.getCashier().getUserId() : null)
                .cashierName(so.getCashier() != null ? so.getCashier().getName() : null)
                .customerId(so.getCustomer() != null ? so.getCustomer().getCustomerId() : null)
                .customerName(so.getCustomer() != null ? so.getCustomer().getName() : null)
                .paymentMethod(so.getPaymentMethod())
                .subtotal(so.getSubtotal())
                .discountAmount(so.getDiscountAmount())
                .taxAmount(so.getTaxAmount())
                .totalAmount(so.getTotalAmount())
                .receivedAmount(so.getReceivedAmount())
                .changeAmount(so.getChangeAmount())
                .build();
    }

    private SalesOrderDetailResponseDTO toDetailDTO(SalesOrderDetail sod) {
        return SalesOrderDetailResponseDTO.builder()
                .sodId(sod.getSodId())
                .productId(sod.getProduct() != null ? sod.getProduct().getProductId() : null)
                .productName(sod.getProduct() != null ? sod.getProduct().getName() : null)
                .barcode(sod.getProduct() != null ? sod.getProduct().getBarcode() : null)
                .quantity(sod.getQuantity())
                .unitPrice(sod.getUnitPrice())
                .costPrice(sod.getCostPrice())
                .discountAmount(sod.getDiscountAmount())
                .totalPrice(sod.getTotalPrice())
                .build();
    }
}

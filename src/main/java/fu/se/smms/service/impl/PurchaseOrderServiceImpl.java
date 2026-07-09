package fu.se.smms.service.impl;

import fu.se.smms.dto.PurchaseOrderDTO;
import fu.se.smms.dto.PurchaseOrderResponseDTO;
import fu.se.smms.entity.Product;
import fu.se.smms.entity.PurchaseOrder;
import fu.se.smms.entity.PurchaseOrderDetail;
import fu.se.smms.entity.Supplier;
import fu.se.smms.entity.User;
import fu.se.smms.enums.OrderStatus;
import fu.se.smms.exception.BadRequestException;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.ProductRepository;
import fu.se.smms.repository.PurchaseOrderRepository;
import fu.se.smms.repository.SupplierRepository;
import fu.se.smms.repository.UserRepository;
import fu.se.smms.service.PurchaseOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public PurchaseOrderServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                                    SupplierRepository supplierRepository,
                                    ProductRepository productRepository,
                                    UserRepository userRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDTO create(PurchaseOrderDTO dto) {
        log.debug("Creating purchase order for supplier id: {}", dto.getSupplierId());

        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy nhà cung cấp: " + dto.getSupplierId()));

        if (!supplier.getStatus()) {
            throw new BadRequestException("Nhà cung cấp đã bị vô hiệu hóa: " + supplier.getName());
        }

        User currentUser = getCurrentUser();

        LocalDateTime expectedDelivery = dto.getExpectedDeliveryDate() != null
                ? dto.getExpectedDeliveryDate().atTime(LocalTime.MIN)
                : null;

        PurchaseOrder po = PurchaseOrder.builder()
                .supplier(supplier)
                .createdBy(currentUser)
                .note(dto.getNote())
                .expectedDeliveryDate(expectedDelivery)
                .status(OrderStatus.DRAFT)
                .purchaseOrderDetails(new ArrayList<>())
                .build();

        Set<Integer> seenProductIds = new HashSet<>();
        for (PurchaseOrderDTO.PurchaseOrderItemDTO item : dto.getItems()) {
            if (!seenProductIds.add(item.getProductId())) {
                throw new BadRequestException("Sản phẩm bị trùng trong đơn hàng: " + item.getProductId());
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PurchaseOrderDetail> details = new ArrayList<>();

        for (PurchaseOrderDTO.PurchaseOrderItemDTO item : dto.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy sản phẩm: " + item.getProductId()));

            BigDecimal unitPrice = product.getCostPrice();
            if (unitPrice == null) {
                throw new BadRequestException("Sản phẩm chưa có giá nhập: " + product.getName());
            }
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            PurchaseOrderDetail detail = PurchaseOrderDetail.builder()
                    .purchaseOrder(po)
                    .product(product)
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(lineTotal)
                    .build();

            details.add(detail);
            totalAmount = totalAmount.add(lineTotal);
        }

        po.setPurchaseOrderDetails(details);
        po.setTotalAmount(totalAmount);

        po = purchaseOrderRepository.save(po);
        log.info("Purchase order created: id={}, totalAmount={}", po.getPoId(), totalAmount);
        return toResponseDTO(po);
    }

    @Override
    public PurchaseOrderResponseDTO findById(Integer id) {
        log.debug("Find purchase order by id: {}", id);
        PurchaseOrder po = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt hàng: " + id));
        return toResponseDTO(po);
    }

    @Override
    public Page<PurchaseOrderResponseDTO> search(Integer supplierId, OrderStatus status, Pageable pageable) {
        return purchaseOrderRepository.search(supplierId, status, pageable)
                .map(this::toSummaryDTO);
    }

    @Override
    @Transactional
    public PurchaseOrderResponseDTO updateStatus(Integer id, OrderStatus newStatus) {
        log.debug("Updating status for PO id: {} to {}", id, newStatus);

        PurchaseOrder po = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt hàng: " + id));

        validateStatusTransition(po.getStatus(), newStatus);

        po.setStatus(newStatus);
        po = purchaseOrderRepository.save(po);
        log.info("Purchase order status updated: id={}, newStatus={}", po.getPoId(), newStatus);
        return toResponseDTO(po);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        boolean valid = switch (current) {
            case DRAFT -> target == OrderStatus.SENT || target == OrderStatus.CANCELLED;
            case SENT -> target == OrderStatus.COMPLETED || target == OrderStatus.CANCELLED;
            default -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    "Không thể chuyển trạng thái từ " + current + " sang " + target);
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user hiện tại"));
    }

    private PurchaseOrderResponseDTO toResponseDTO(PurchaseOrder po) {
        List<PurchaseOrderResponseDTO.PurchaseOrderDetailResponseDTO> details = null;
        if (po.getPurchaseOrderDetails() != null) {
            details = po.getPurchaseOrderDetails().stream()
                    .map(d -> PurchaseOrderResponseDTO.PurchaseOrderDetailResponseDTO.builder()
                            .podId(d.getPodId())
                            .productId(d.getProduct().getProductId())
                            .productName(d.getProduct().getName())
                            .barcode(d.getProduct().getBarcode())
                            .quantity(d.getQuantity())
                            .unitPrice(d.getUnitPrice())
                            .totalPrice(d.getTotalPrice())
                            .build())
                    .toList();
        }

        return PurchaseOrderResponseDTO.builder()
                .poId(po.getPoId())
                .orderDate(po.getOrderDate())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .note(po.getNote())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .supplierId(po.getSupplier().getSupplierId())
                .supplierName(po.getSupplier().getName())
                .createdByUserId(po.getCreatedBy().getUserId())
                .createdByName(po.getCreatedBy().getName())
                .details(details)
                .build();
    }

    private PurchaseOrderResponseDTO toSummaryDTO(PurchaseOrder po) {
        return PurchaseOrderResponseDTO.builder()
                .poId(po.getPoId())
                .orderDate(po.getOrderDate())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .note(po.getNote())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .supplierId(po.getSupplier().getSupplierId())
                .supplierName(po.getSupplier().getName())
                .createdByUserId(po.getCreatedBy().getUserId())
                .createdByName(po.getCreatedBy().getName())
                .build();
    }
}

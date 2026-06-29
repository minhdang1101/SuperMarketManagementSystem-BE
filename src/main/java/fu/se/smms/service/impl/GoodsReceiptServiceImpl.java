package fu.se.smms.service.impl;

import fu.se.smms.dto.GoodsReceiptDTO;
import fu.se.smms.dto.GoodsReceiptResponseDTO;
import fu.se.smms.entity.*;
import fu.se.smms.enums.GoodsReceiptStatus;
import fu.se.smms.enums.OrderStatus;
import fu.se.smms.exception.BadRequestException;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.*;
import fu.se.smms.service.GoodsReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GoodsReceiptServiceImpl implements GoodsReceiptService {
    private static final Logger log = LoggerFactory.getLogger(GoodsReceiptServiceImpl.class);

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public GoodsReceiptServiceImpl(GoodsReceiptRepository goodsReceiptRepository,
                                   PurchaseOrderRepository purchaseOrderRepository,
                                   ProductRepository productRepository,
                                   UserRepository userRepository) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public GoodsReceiptResponseDTO receiveGoods(GoodsReceiptDTO dto) {
        log.debug("Receiving goods for PO id: {}", dto.getPoId());

        PurchaseOrder po = purchaseOrderRepository.findByIdWithDetails(dto.getPoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đặt hàng: " + dto.getPoId()));

        if (po.getStatus() != OrderStatus.SENT) {
            throw new BadRequestException(
                    "Chỉ có thể nhận hàng cho đơn ở trạng thái SENT. Trạng thái hiện tại: " + po.getStatus());
        }

        Set<Integer> seenProductIds = new HashSet<>();
        for (GoodsReceiptDTO.GoodsReceiptItemDTO item : dto.getItems()) {
            if (!seenProductIds.add(item.getProductId())) {
                throw new BadRequestException("Sản phẩm bị trùng trong phiếu nhận hàng: " + item.getProductId());
            }
        }

        Map<Integer, PurchaseOrderDetail> orderedItemsMap = po.getPurchaseOrderDetails().stream()
                .collect(Collectors.toMap(
                        d -> d.getProduct().getProductId(),
                        Function.identity()));

        User currentUser = getCurrentUser();

        GoodsReceipt receipt = GoodsReceipt.builder()
                .purchaseOrder(po)
                .receivedBy(currentUser)
                .status(GoodsReceiptStatus.COMPLETED)
                .note(dto.getNote())
                .goodsReceiptDetails(new ArrayList<>())
                .build();

        List<GoodsReceiptDetail> receiptDetails = new ArrayList<>();

        for (GoodsReceiptDTO.GoodsReceiptItemDTO item : dto.getItems()) {
            PurchaseOrderDetail orderedDetail = orderedItemsMap.get(item.getProductId());
            if (orderedDetail == null) {
                throw new BadRequestException(
                        "Sản phẩm ID " + item.getProductId() + " không thuộc đơn đặt hàng này");
            }

            int orderedQty = orderedDetail.getQuantity() != null ? orderedDetail.getQuantity() : 0;
            if (item.getReceivedQuantity() < 0) {
                throw new BadRequestException(
                        "Số lượng nhận không được âm cho sản phẩm ID " + item.getProductId());
            }

            Product product = orderedDetail.getProduct();

            GoodsReceiptDetail detail = GoodsReceiptDetail.builder()
                    .goodsReceipt(receipt)
                    .product(product)
                    .orderedQuantity(orderedQty)
                    .receivedQuantity(item.getReceivedQuantity())
                    .expiryDate(item.getExpiryDate())
                    .batchNumber(item.getBatchNumber())
                    .build();

            receiptDetails.add(detail);

            int currentStock = product.getStockLevel() != null ? product.getStockLevel() : 0;
            product.setStockLevel(currentStock + item.getReceivedQuantity());
            productRepository.save(product);

            log.info("Stock updated: productId={}, added={}, newStock={}",
                    product.getProductId(), item.getReceivedQuantity(), product.getStockLevel());
        }

        receipt.setGoodsReceiptDetails(receiptDetails);
        receipt = goodsReceiptRepository.save(receipt);

        po.setStatus(OrderStatus.COMPLETED);
        purchaseOrderRepository.save(po);

        log.info("Goods receipt created: id={}, poId={}", receipt.getReceiptId(), po.getPoId());
        return toResponseDTO(receipt);
    }

    @Override
    public GoodsReceiptResponseDTO findById(Integer id) {
        log.debug("Find goods receipt by id: {}", id);
        GoodsReceipt receipt = goodsReceiptRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhận hàng: " + id));
        return toResponseDTO(receipt);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user hiện tại"));
    }

    private GoodsReceiptResponseDTO toResponseDTO(GoodsReceipt receipt) {
        List<GoodsReceiptResponseDTO.GoodsReceiptDetailResponseDTO> details = null;
        if (receipt.getGoodsReceiptDetails() != null) {
            details = receipt.getGoodsReceiptDetails().stream()
                    .map(d -> GoodsReceiptResponseDTO.GoodsReceiptDetailResponseDTO.builder()
                            .grdId(d.getGrdId())
                            .productId(d.getProduct().getProductId())
                            .productName(d.getProduct().getName())
                            .barcode(d.getProduct().getBarcode())
                            .orderedQuantity(d.getOrderedQuantity())
                            .receivedQuantity(d.getReceivedQuantity())
                            .expiryDate(d.getExpiryDate())
                            .batchNumber(d.getBatchNumber())
                            .build())
                    .toList();
        }

        return GoodsReceiptResponseDTO.builder()
                .receiptId(receipt.getReceiptId())
                .poId(receipt.getPurchaseOrder().getPoId())
                .receivedDate(receipt.getReceivedDate())
                .status(receipt.getStatus())
                .note(receipt.getNote())
                .receivedByUserId(receipt.getReceivedBy().getUserId())
                .receivedByName(receipt.getReceivedBy().getName())
                .details(details)
                .build();
    }
}

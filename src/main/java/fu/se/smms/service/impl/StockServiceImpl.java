package fu.se.smms.service.impl;

import fu.se.smms.dto.StockAdjustmentDTO;
import fu.se.smms.dto.StockAdjustmentResponseDTO;
import fu.se.smms.entity.Product;
import fu.se.smms.entity.StockAdjustment;
import fu.se.smms.entity.User;
import fu.se.smms.exception.BadRequestException;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.ProductRepository;
import fu.se.smms.repository.StockAdjustmentRepository;
import fu.se.smms.repository.UserRepository;
import fu.se.smms.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockServiceImpl implements StockService {
    private static final Logger log = LoggerFactory.getLogger(StockServiceImpl.class);

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public StockServiceImpl(StockAdjustmentRepository stockAdjustmentRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository) {
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public StockAdjustmentResponseDTO adjustStock(StockAdjustmentDTO dto) {
        log.debug("Adjusting stock for product id: {}, quantity: {}, reason: {}",
                dto.getProductId(), dto.getQuantity(), dto.getReason());

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm: " + dto.getProductId()));

        if (product.getStockLevel() < dto.getQuantity()) {
            throw new BadRequestException(
                    "Số lượng trừ (" + dto.getQuantity() + ") vượt quá tồn kho hiện tại (" + product.getStockLevel() + ")");
        }

        User currentUser = getCurrentUser();

        int previousStock = product.getStockLevel();
        product.setStockLevel(previousStock - dto.getQuantity());
        productRepository.save(product);

        StockAdjustment adjustment = StockAdjustment.builder()
                .product(product)
                .adjustedBy(currentUser)
                .quantity(dto.getQuantity())
                .reason(dto.getReason())
                .note(dto.getNote())
                .build();

        adjustment = stockAdjustmentRepository.save(adjustment);

        log.info("Stock adjusted: productId={}, deducted={}, previousStock={}, newStock={}",
                product.getProductId(), dto.getQuantity(), previousStock, product.getStockLevel());

        return toResponseDTO(adjustment, product.getStockLevel());
    }

    @Override
    public Page<StockAdjustmentResponseDTO> getAdjustmentHistory(Integer productId, Pageable pageable) {
        return stockAdjustmentRepository.findByProductId(productId, pageable)
                .map(sa -> toResponseDTO(sa, null));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user hiện tại"));
    }

    private StockAdjustmentResponseDTO toResponseDTO(StockAdjustment sa, Integer stockAfter) {
        return StockAdjustmentResponseDTO.builder()
                .adjustmentId(sa.getAdjustmentId())
                .productId(sa.getProduct().getProductId())
                .productName(sa.getProduct().getName())
                .barcode(sa.getProduct().getBarcode())
                .quantity(sa.getQuantity())
                .reason(sa.getReason())
                .note(sa.getNote())
                .adjustedAt(sa.getAdjustedAt())
                .adjustedByUserId(sa.getAdjustedBy().getUserId())
                .adjustedByName(sa.getAdjustedBy().getName())
                .stockAfterAdjustment(stockAfter)
                .build();
    }
}

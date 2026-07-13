package fu.se.smms.service.impl;

import fu.se.smms.dto.InventoryStockDTO;
import fu.se.smms.entity.Product;
import fu.se.smms.enums.StockStatus;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.GoodsReceiptDetailRepository;
import fu.se.smms.repository.ProductRepository;
import fu.se.smms.repository.SalesOrderDetailRepository;
import fu.se.smms.repository.StockAdjustmentRepository;
import fu.se.smms.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ProductRepository productRepository;
    private final GoodsReceiptDetailRepository goodsReceiptDetailRepository;
    private final SalesOrderDetailRepository salesOrderDetailRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;

    public InventoryServiceImpl(ProductRepository productRepository,
                                GoodsReceiptDetailRepository goodsReceiptDetailRepository,
                                SalesOrderDetailRepository salesOrderDetailRepository,
                                StockAdjustmentRepository stockAdjustmentRepository) {
        this.productRepository = productRepository;
        this.goodsReceiptDetailRepository = goodsReceiptDetailRepository;
        this.salesOrderDetailRepository = salesOrderDetailRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
    }

    @Override
    public List<InventoryStockDTO> getAllStockInfo() {
        log.debug("Calculating stock info for all products");

        List<Product> products = productRepository.findAll();
        Map<Integer, Integer> importedMap = toMap(goodsReceiptDetailRepository.sumReceivedQuantityGroupByProduct());
        Map<Integer, Integer> exportedMap = toMap(salesOrderDetailRepository.sumSoldQuantityGroupByProduct());
        Map<Integer, Integer> adjustedMap = toMap(stockAdjustmentRepository.sumAdjustmentGroupByProduct());

        return products.stream()
                .map(product -> {
                    int imported = importedMap.getOrDefault(product.getProductId(), 0);
                    int exported = exportedMap.getOrDefault(product.getProductId(), 0);
                    int adjusted = adjustedMap.getOrDefault(product.getProductId(), 0);
                    return buildStockDTO(product, imported, exported, adjusted);
                })
                .toList();
    }

    @Override
    public InventoryStockDTO getStockInfo(Integer productId) {
        log.debug("Calculating stock info for product id: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + productId));

        int imported = goodsReceiptDetailRepository.sumReceivedQuantityByProductId(productId);
        int exported = salesOrderDetailRepository.sumSoldQuantityByProductId(productId);
        int adjusted = stockAdjustmentRepository.sumAdjustmentByProductId(productId);

        return buildStockDTO(product, imported, exported, adjusted);
    }

    @Override
    public List<InventoryStockDTO> getLowStockProducts() {
        log.debug("Fetching low stock products");
        return getAllStockInfo().stream()
                .filter(dto -> dto.getStockStatus() == StockStatus.LOW_STOCK
                            || dto.getStockStatus() == StockStatus.OUT_OF_STOCK)
                .toList();
    }

    private InventoryStockDTO buildStockDTO(Product product, int imported, int exported, int adjusted) {
        int currentStock = product.getStockLevel() != null ? product.getStockLevel() : 0;

        return InventoryStockDTO.builder()
                .productId(product.getProductId())
                .productName(product.getName())
                .barcode(product.getBarcode())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .totalImported(imported)
                .totalExported(exported)
                .totalAdjusted(adjusted)
                .currentStock(currentStock)
                .minStockLevel(product.getMinStockLevel())
                .maxStockLevel(product.getMaxStockLevel())
                .stockStatus(resolveStockStatus(currentStock, product.getMinStockLevel(), product.getMaxStockLevel()))
                .build();
    }

    private StockStatus resolveStockStatus(int currentStock, Integer minLevel, Integer maxLevel) {
        if (currentStock <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (minLevel != null && currentStock < minLevel) {
            return StockStatus.LOW_STOCK;
        }
        if (maxLevel != null && currentStock > maxLevel) {
            return StockStatus.OVER_STOCK;
        }
        return StockStatus.NORMAL;
    }

    private Map<Integer, Integer> toMap(List<Object[]> rows) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            Integer productId = (Integer) row[0];
            Integer qty = ((Number) row[1]).intValue();
            map.put(productId, qty);
        }
        return map;
    }
}

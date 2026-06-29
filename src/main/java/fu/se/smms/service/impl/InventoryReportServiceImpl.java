package fu.se.smms.service.impl;

import fu.se.smms.dto.ExpiredGoodsItemDTO;
import fu.se.smms.dto.LowStockItemDTO;
import fu.se.smms.dto.StockMovementItemDTO;
import fu.se.smms.entity.GoodsReceiptDetail;
import fu.se.smms.entity.Product;
import fu.se.smms.repository.GoodsReceiptDetailRepository;
import fu.se.smms.repository.ProductRepository;
import fu.se.smms.repository.SalesOrderDetailRepository;
import fu.se.smms.repository.StockAdjustmentRepository;
import fu.se.smms.service.InventoryReportService;
import fu.se.smms.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class InventoryReportServiceImpl implements InventoryReportService {
    private static final Logger log = LoggerFactory.getLogger(InventoryReportServiceImpl.class);
    private static final int EXPIRY_WARNING_DAYS = 7;

    private final ProductRepository productRepository;
    private final GoodsReceiptDetailRepository goodsReceiptDetailRepository;
    private final SalesOrderDetailRepository salesOrderDetailRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final InventoryService inventoryService;

    public InventoryReportServiceImpl(ProductRepository productRepository,
                                      GoodsReceiptDetailRepository goodsReceiptDetailRepository,
                                      SalesOrderDetailRepository salesOrderDetailRepository,
                                      StockAdjustmentRepository stockAdjustmentRepository,
                                      InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.goodsReceiptDetailRepository = goodsReceiptDetailRepository;
        this.salesOrderDetailRepository = salesOrderDetailRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.inventoryService = inventoryService;
    }

    // BR-03: Stock Movement includes Purchases, Sales, Adjustments (Returns, Damages, etc.)
    @Override
    public List<StockMovementItemDTO> getStockMovementReport(LocalDateTime dateFrom,
                                                             LocalDateTime dateTo,
                                                             Integer categoryId) {
        log.debug("Generating Stock Movement report: {} to {}, categoryId={}", dateFrom, dateTo, categoryId);

        Map<Integer, Integer> importedMap = toMap(
                goodsReceiptDetailRepository.sumReceivedQuantityByDateRange(dateFrom, dateTo, categoryId));
        Map<Integer, Integer> exportedMap = toMap(
                salesOrderDetailRepository.sumSoldQuantityByDateRange(dateFrom, dateTo, categoryId));
        Map<Integer, Integer> adjustedMap = toMap(
                stockAdjustmentRepository.sumAdjustmentByDateRange(dateFrom, dateTo, categoryId));

        Set<Integer> allProductIds = new HashSet<>();
        allProductIds.addAll(importedMap.keySet());
        allProductIds.addAll(exportedMap.keySet());
        allProductIds.addAll(adjustedMap.keySet());

        if (allProductIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, Product> productMap = productRepository.findAllById(allProductIds).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        return allProductIds.stream()
                .map(pid -> {
                    Product product = productMap.get(pid);
                    int stockIn = importedMap.getOrDefault(pid, 0);
                    int stockOut = exportedMap.getOrDefault(pid, 0);
                    int adjustments = adjustedMap.getOrDefault(pid, 0);

                    return StockMovementItemDTO.builder()
                            .productId(pid)
                            .productName(product != null ? product.getName() : null)
                            .categoryName(product != null && product.getCategory() != null
                                    ? product.getCategory().getName() : null)
                            .stockIn(stockIn)
                            .stockOut(stockOut)
                            .adjustments(adjustments)
                            .netChange(stockIn - stockOut - adjustments)
                            .build();
                })
                .sorted(Comparator.comparing(StockMovementItemDTO::getProductId))
                .toList();
    }

    // BR-01: Low Stock threshold is defined per product (minStockLevel)
    @Override
    public List<LowStockItemDTO> getLowStockReport() {
        log.debug("Generating Low Stock report");

        return inventoryService.getAllStockInfo().stream()
                .filter(stock -> stock.getMinStockLevel() != null && stock.getCurrentStock() < stock.getMinStockLevel())
                .map(stock -> LowStockItemDTO.builder()
                        .productId(stock.getProductId())
                        .productName(stock.getProductName())
                        .categoryName(stock.getCategoryName())
                        .currentStock(stock.getCurrentStock())
                        .reorderLevel(stock.getMinStockLevel())
                        .shortage(stock.getMinStockLevel() - stock.getCurrentStock())
                        .build())
                .sorted(Comparator.comparing(LowStockItemDTO::getShortage).reversed())
                .toList();
    }

    // BR-02: Expired Goods includes items expiring within next 7 days
    @Override
    public List<ExpiredGoodsItemDTO> getExpiredGoodsReport() {
        log.debug("Generating Expired Goods report");

        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(EXPIRY_WARNING_DAYS);

        List<GoodsReceiptDetail> expiredItems =
                goodsReceiptDetailRepository.findExpiredOrExpiringSoon(thresholdDate);

        return expiredItems.stream()
                .map(grd -> {
                    String status = grd.getExpiryDate().isBefore(today) || grd.getExpiryDate().isEqual(today)
                            ? "EXPIRED"
                            : "EXPIRING_SOON";

                    return ExpiredGoodsItemDTO.builder()
                            .productId(grd.getProduct().getProductId())
                            .productName(grd.getProduct().getName())
                            .batchNumber(grd.getBatchNumber())
                            .expiryDate(grd.getExpiryDate())
                            .quantity(grd.getReceivedQuantity())
                            .status(status)
                            .build();
                })
                .sorted(Comparator.comparing(ExpiredGoodsItemDTO::getExpiryDate))
                .toList();
    }

    private Map<Integer, Integer> toMap(List<Object[]> rows) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Integer) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }
}

package fu.se.smms.service.impl;

import fu.se.smms.dto.DashboardSummaryDTO;
import fu.se.smms.dto.DashboardSummaryDTO.*;
import fu.se.smms.entity.SalesOrder;
import fu.se.smms.enums.OrderStatus;
import fu.se.smms.repository.*;
import fu.se.smms.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final Set<String> VALID_PERIODS = Set.of("today", "week", "month");
    
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderDetailRepository salesOrderDetailRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptDetailRepository goodsReceiptDetailRepository;

    public DashboardServiceImpl(
            SalesOrderRepository salesOrderRepository,
            SalesOrderDetailRepository salesOrderDetailRepository,
            ProductRepository productRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            GoodsReceiptDetailRepository goodsReceiptDetailRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderDetailRepository = salesOrderDetailRepository;
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptDetailRepository = goodsReceiptDetailRepository;
    }

    @Override
    public DashboardSummaryDTO getSummary(String period) {
        String normalizedPeriod = validateAndNormalizePeriod(period);
        
        LocalDateTime[] dateRange = getDateRange(normalizedPeriod);
        LocalDateTime startDate = dateRange[0];
        LocalDateTime endDate = dateRange[1];
        
        LocalDateTime[] prevRange = getPreviousDateRange(normalizedPeriod);
        LocalDateTime prevStartDate = prevRange[0];
        LocalDateTime prevEndDate = prevRange[1];
        
        log.info("Dashboard summary: period={}, range=[{} to {}]", normalizedPeriod, startDate, endDate);

        BigDecimal totalRevenue = nullSafe(salesOrderRepository.findTotalRevenue(startDate, endDate));
        Long ordersProcessed = nullSafe(salesOrderRepository.countOrders(startDate, endDate));
        
        BigDecimal prevRevenue = nullSafe(salesOrderRepository.findTotalRevenue(prevStartDate, prevEndDate));
        Long prevOrders = nullSafe(salesOrderRepository.countOrders(prevStartDate, prevEndDate));
        
        BigDecimal revenueChange = calculatePercentageChange(totalRevenue, prevRevenue);
        BigDecimal ordersChange = calculatePercentageChange(
            BigDecimal.valueOf(ordersProcessed), 
            BigDecimal.valueOf(prevOrders)
        );
        
        long dayCount = Math.max(1, getDayCount(normalizedPeriod));
        BigDecimal dailySales = totalRevenue.divide(BigDecimal.valueOf(dayCount), 0, RoundingMode.HALF_UP);
        
        Integer lowStockCount = nullSafe(productRepository.countLowStockProducts());
        Integer expiringCount = nullSafe(goodsReceiptDetailRepository.countExpiringProducts(
            LocalDate.now().plusDays(7)
        ));
        Integer pendingPOCount = nullSafe(purchaseOrderRepository.countByStatus(OrderStatus.SENT));
        
        List<SalesTrendItem> salesTrend = buildSalesTrend(normalizedPeriod, startDate, endDate);
        List<TopProductItem> topProducts = buildTopProducts(startDate, endDate, prevStartDate, prevEndDate);
        List<RecentTransactionItem> recentTransactions = buildRecentTransactions();

        return DashboardSummaryDTO.builder()
                .totalRevenue(totalRevenue)
                .revenueChange(revenueChange)
                .dailySales(dailySales)
                .dailySalesChange(revenueChange)
                .ordersProcessed(ordersProcessed)
                .ordersChange(ordersChange)
                .lowStockCount(lowStockCount)
                .expiringProductsCount(expiringCount)
                .pendingPurchaseOrders(pendingPOCount)
                .salesTrend(salesTrend)
                .topProducts(topProducts)
                .recentTransactions(recentTransactions)
                .build();
    }

    private String validateAndNormalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "today";
        }
        String normalized = period.toLowerCase().trim();
        if (!VALID_PERIODS.contains(normalized)) {
            throw new IllegalArgumentException(
                "Invalid period. Must be one of: " + VALID_PERIODS + ". Got: " + period
            );
        }
        return normalized;
    }

    private LocalDateTime[] getDateRange(String period) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "week" -> new LocalDateTime[]{
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(),
                today.plusDays(1).atStartOfDay()
            };
            case "month" -> new LocalDateTime[]{
                today.withDayOfMonth(1).atStartOfDay(),
                today.plusDays(1).atStartOfDay()
            };
            default -> new LocalDateTime[]{
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
            };
        };
    }

    private LocalDateTime[] getPreviousDateRange(String period) {
        LocalDate today = LocalDate.now();
        return switch (period) {
            case "week" -> {
                LocalDate prevWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
                yield new LocalDateTime[]{
                    prevWeekStart.atStartOfDay(),
                    prevWeekStart.plusWeeks(1).atStartOfDay()
                };
            }
            case "month" -> {
                LocalDate prevMonthStart = today.withDayOfMonth(1).minusMonths(1);
                yield new LocalDateTime[]{
                    prevMonthStart.atStartOfDay(),
                    prevMonthStart.plusMonths(1).atStartOfDay()
                };
            }
            default -> new LocalDateTime[]{
                today.minusDays(1).atStartOfDay(),
                today.atStartOfDay()
            };
        };
    }

    private long getDayCount(String period) {
        return switch (period) {
            case "week" -> 7;
            case "month" -> LocalDate.now().lengthOfMonth();
            default -> 1;
        };
    }

    private BigDecimal calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current == null || current.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(100);
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private List<SalesTrendItem> buildSalesTrend(String period, LocalDateTime start, LocalDateTime end) {
        List<SalesTrendItem> trend = new ArrayList<>();
        DateTimeFormatter formatter;
        
        switch (period) {
            case "week" -> {
                formatter = DateTimeFormatter.ofPattern("EEE");
                for (int i = 0; i < 7; i++) {
                    LocalDateTime dayStart = start.plusDays(i);
                    LocalDateTime dayEnd = dayStart.plusDays(1);
                    
                    if (dayEnd.isAfter(end)) dayEnd = end;
                    
                    BigDecimal revenue = nullSafe(salesOrderRepository.findTotalRevenue(dayStart, dayEnd));
                    BigDecimal cogs = nullSafe(salesOrderDetailRepository.findTotalCogs(dayStart, dayEnd));
                    Long orders = nullSafe(salesOrderRepository.countOrders(dayStart, dayEnd));
                    
                    trend.add(SalesTrendItem.builder()
                            .label(dayStart.format(formatter))
                            .revenue(revenue)
                            .profit(revenue.subtract(cogs))
                            .orderCount(orders)
                            .build());
                }
            }
            case "month" -> {
                LocalDateTime weekStart = start;
                int weekNum = 1;
                while (weekStart.isBefore(end) && weekNum <= 5) {
                    LocalDateTime weekEnd = weekStart.plusWeeks(1);
                    if (weekEnd.isAfter(end)) weekEnd = end;
                    
                    BigDecimal revenue = nullSafe(salesOrderRepository.findTotalRevenue(weekStart, weekEnd));
                    BigDecimal cogs = nullSafe(salesOrderDetailRepository.findTotalCogs(weekStart, weekEnd));
                    Long orders = nullSafe(salesOrderRepository.countOrders(weekStart, weekEnd));
                    
                    trend.add(SalesTrendItem.builder()
                            .label("Tuần " + weekNum)
                            .revenue(revenue)
                            .profit(revenue.subtract(cogs))
                            .orderCount(orders)
                            .build());
                    
                    weekStart = weekEnd;
                    weekNum++;
                }
            }
            default -> { // today - by hour blocks
                for (int hour = 6; hour <= 21; hour += 3) {
                    LocalDateTime hourStart = start.withHour(hour);
                    LocalDateTime hourEnd = hourStart.plusHours(3);
                    
                    BigDecimal revenue = nullSafe(salesOrderRepository.findTotalRevenue(hourStart, hourEnd));
                    Long orders = nullSafe(salesOrderRepository.countOrders(hourStart, hourEnd));
                    
                    trend.add(SalesTrendItem.builder()
                            .label(String.format("%02d:00", hour))
                            .revenue(revenue)
                            .profit(BigDecimal.ZERO)
                            .orderCount(orders)
                            .build());
                }
            }
        }
        return trend;
    }

    private List<TopProductItem> buildTopProducts(
            LocalDateTime start, LocalDateTime end,
            LocalDateTime prevStart, LocalDateTime prevEnd) {
        
        List<Object[]> rows = salesOrderDetailRepository.findTopSellingProducts(
            start, end, PageRequest.of(0, 5)
        );
        List<TopProductItem> products = new ArrayList<>();
        
        for (Object[] row : rows) {
            Integer productId = (Integer) row[0];
            String productName = (String) row[1];
            String categoryName = (String) row[2];
            Long unitsSold = (Long) row[3];
            BigDecimal revenue = (BigDecimal) row[4];
            Integer stockLevel = (Integer) row[5];
            Integer minStockLevel = row[6] != null ? (Integer) row[6] : 10;
            
            // Calculate previous period sales for change %
            Long prevUnitsSold = salesOrderDetailRepository.countUnitsSoldByProduct(productId, prevStart, prevEnd);
            BigDecimal changePercent = calculatePercentageChange(
                BigDecimal.valueOf(unitsSold),
                BigDecimal.valueOf(prevUnitsSold != null ? prevUnitsSold : 0)
            );
            
            // Determine stock status
            String stockStatus;
            if (stockLevel <= 0) {
                stockStatus = "CRITICAL";
            } else if (stockLevel < minStockLevel) {
                stockStatus = "LOW_STOCK";
            } else {
                stockStatus = "IN_STOCK";
            }
            
            products.add(TopProductItem.builder()
                    .productId(productId)
                    .productName(productName)
                    .categoryName(categoryName)
                    .unitsSold(unitsSold.intValue())
                    .revenue(revenue)
                    .stockStatus(stockStatus)
                    .changePercent(changePercent)
                    .build());
        }
        return products;
    }

    private List<RecentTransactionItem> buildRecentTransactions() {
        List<SalesOrder> orders = salesOrderRepository.findTop10RecentOrders();
        List<RecentTransactionItem> transactions = new ArrayList<>();
        
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        for (SalesOrder order : orders) {
            transactions.add(RecentTransactionItem.builder()
                    .orderId(order.getSalesOrderId())
                    .invoiceNumber(order.getInvoiceNumber())
                    .customerName(order.getCustomer() != null ? order.getCustomer().getName() : "Khách lẻ")
                    .itemCount(order.getSalesOrderDetails() != null ? order.getSalesOrderDetails().size() : 0)
                    .totalAmount(order.getTotalAmount())
                    .paymentMethod(order.getPaymentMethod().name())
                    .cashierName(order.getCashier() != null ? order.getCashier().getName() : "N/A")
                    .time(order.getOrderDate().format(timeFormatter))
                    .build());
        }
        return transactions;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
    
    private Long nullSafe(Long value) {
        return value != null ? value : 0L;
    }
    
    private Integer nullSafe(Integer value) {
        return value != null ? value : 0;
    }
}

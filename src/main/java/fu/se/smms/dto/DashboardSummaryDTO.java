package fu.se.smms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    

    private BigDecimal totalRevenue;
    private BigDecimal revenueChange;
    private BigDecimal dailySales;
    private BigDecimal dailySalesChange;
    private Long ordersProcessed;
    private BigDecimal ordersChange;
    private Integer lowStockCount;
    private Integer expiringProductsCount;
    private Integer pendingPurchaseOrders;
    

    private List<SalesTrendItem> salesTrend;

    private List<TopProductItem> topProducts;
    private List<RecentTransactionItem> recentTransactions;
    

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesTrendItem {
        private String label;
        private BigDecimal revenue;
        private BigDecimal profit;
        private Long orderCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductItem {
        private Integer productId;
        private String productName;
        private String categoryName;
        private Integer unitsSold;
        private BigDecimal revenue;
        private String stockStatus;
        private BigDecimal changePercent;
    }
    

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTransactionItem {
        private Integer orderId;
        private String invoiceNumber;
        private String customerName;
        private Integer itemCount;
        private BigDecimal totalAmount;
        private String paymentMethod;
        private String cashierName;
        private String time;
    }
}

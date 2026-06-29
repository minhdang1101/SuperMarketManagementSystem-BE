package fu.se.smms.service.impl;

import fu.se.smms.dto.ReportDetailResDTO;
import fu.se.smms.dto.ReportFilterReq;
import fu.se.smms.dto.ReportSummaryResDTO;
import fu.se.smms.enums.ReportGroupBy;
import fu.se.smms.exception.BusinessRuleException;
import fu.se.smms.repository.SalesOrderDetailRepository;
import fu.se.smms.repository.SalesOrderRepository;
import fu.se.smms.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);
    private static final long MAX_REPORT_DATE_RANGE_DAYS = 365;

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderDetailRepository salesOrderDetailRepository;

    public ReportServiceImpl(SalesOrderRepository salesOrderRepository,
                             SalesOrderDetailRepository salesOrderDetailRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderDetailRepository = salesOrderDetailRepository;
    }

    @Override
    public ReportSummaryResDTO getRevenueReport(ReportFilterReq filter) {
        validateDateRange(filter.getDateFrom(), filter.getDateTo());

        LocalDateTime startDate = filter.getDateFrom().atStartOfDay();
        LocalDateTime endDate = filter.getDateTo().plusDays(1).atStartOfDay();

        log.info("Revenue report request: dateFrom={}, dateTo={}, groupBy={}, startDateTime={}, endDateTime={}",
                filter.getDateFrom(), filter.getDateTo(), filter.getGroupBy(), startDate, endDate);

        Long totalOrders = salesOrderRepository.countOrders(startDate, endDate);
        BigDecimal totalRevenue = salesOrderRepository.findTotalRevenue(startDate, endDate);
        BigDecimal totalCogs = salesOrderDetailRepository.findTotalCogs(startDate, endDate);
        
        // Ensure non-null values
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        if (totalCogs == null) totalCogs = BigDecimal.ZERO;
        
        BigDecimal grossProfit = totalRevenue.subtract(totalCogs);
        BigDecimal profitMargin = calculateProfitMargin(grossProfit, totalRevenue);

        log.info("Revenue report result: totalOrders={}, totalRevenue={}, totalCogs={}, grossProfit={}, profitMargin={}",
                totalOrders, totalRevenue, totalCogs, grossProfit, profitMargin);

        List<ReportDetailResDTO> details = Collections.emptyList();
        if (filter.getGroupBy() != null) {
            details = fetchGroupedDetails(filter.getGroupBy(), startDate, endDate);
        }

        return ReportSummaryResDTO.builder()
                .dateFrom(filter.getDateFrom())
                .dateTo(filter.getDateTo())
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue.setScale(0, RoundingMode.HALF_UP))
                .totalCogs(totalCogs.setScale(0, RoundingMode.HALF_UP))
                .grossProfit(grossProfit.setScale(0, RoundingMode.HALF_UP))
                .profitMargin(profitMargin)
                .details(details)
                .build();
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null) {
            throw new BusinessRuleException("MSG05", "Ngày bắt đầu và ngày kết thúc là bắt buộc");
        }
        if (dateTo.isBefore(dateFrom)) {
            throw new BusinessRuleException("MSG03", "Ngày kết thúc phải >= ngày bắt đầu");
        }
        long daysBetween = ChronoUnit.DAYS.between(dateFrom, dateTo);
        if (daysBetween > MAX_REPORT_DATE_RANGE_DAYS) {
            throw new BusinessRuleException("MSG03",
                    "BR-06: Khoảng cách ngày báo cáo không được vượt quá " + MAX_REPORT_DATE_RANGE_DAYS
                            + " ngày. Hiện tại: " + daysBetween + " ngày");
        }
    }

    private BigDecimal calculateProfitMargin(BigDecimal grossProfit, BigDecimal revenue) {
        if (revenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return grossProfit
                .multiply(BigDecimal.valueOf(100))
                .divide(revenue, 2, RoundingMode.HALF_UP);
    }

    private List<ReportDetailResDTO> fetchGroupedDetails(ReportGroupBy groupBy,
                                                         LocalDateTime startDate,
                                                         LocalDateTime endDate) {
        List<Object[]> rows = switch (groupBy) {
            case CATEGORY -> salesOrderDetailRepository.findRevenueGroupByCategory(startDate, endDate);
            case PRODUCT -> salesOrderDetailRepository.findRevenueGroupByProduct(startDate, endDate);
            case CASHIER -> salesOrderDetailRepository.findRevenueGroupByCashier(startDate, endDate);
        };

        return rows.stream().map(this::mapToDetailDTO).toList();
    }

    private ReportDetailResDTO mapToDetailDTO(Object[] row) {
        String groupName = (String) row[0];
        BigDecimal revenue = (BigDecimal) row[1];
        BigDecimal cogs = (BigDecimal) row[2];
        Long orderCount = (Long) row[3];
        BigDecimal grossProfit = revenue.subtract(cogs);
        BigDecimal profitMargin = calculateProfitMargin(grossProfit, revenue);

        return ReportDetailResDTO.builder()
                .groupName(groupName)
                .revenue(revenue.setScale(0, RoundingMode.HALF_UP))
                .cogs(cogs.setScale(0, RoundingMode.HALF_UP))
                .grossProfit(grossProfit.setScale(0, RoundingMode.HALF_UP))
                .profitMargin(profitMargin)
                .orderCount(orderCount)
                .build();
    }
}

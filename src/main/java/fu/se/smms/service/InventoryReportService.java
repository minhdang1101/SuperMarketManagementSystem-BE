package fu.se.smms.service;

import fu.se.smms.dto.ExpiredGoodsItemDTO;
import fu.se.smms.dto.LowStockItemDTO;
import fu.se.smms.dto.StockMovementItemDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryReportService {

    List<StockMovementItemDTO> getStockMovementReport(LocalDateTime dateFrom, LocalDateTime dateTo, Integer categoryId);

    List<LowStockItemDTO> getLowStockReport();

    List<ExpiredGoodsItemDTO> getExpiredGoodsReport();
}

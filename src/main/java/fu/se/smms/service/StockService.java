package fu.se.smms.service;

import fu.se.smms.dto.StockAdjustmentDTO;
import fu.se.smms.dto.StockAdjustmentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockService {

    StockAdjustmentResponseDTO adjustStock(StockAdjustmentDTO dto);

    Page<StockAdjustmentResponseDTO> getAdjustmentHistory(Integer productId, Pageable pageable);
}

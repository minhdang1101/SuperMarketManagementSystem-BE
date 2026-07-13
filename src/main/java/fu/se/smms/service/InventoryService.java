package fu.se.smms.service;

import fu.se.smms.dto.InventoryStockDTO;

import java.util.List;

public interface InventoryService {

    List<InventoryStockDTO> getAllStockInfo();

    InventoryStockDTO getStockInfo(Integer productId);

    List<InventoryStockDTO> getLowStockProducts();
}

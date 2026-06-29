package fu.se.smms.service;

import fu.se.smms.dto.GoodsReceiptDTO;
import fu.se.smms.dto.GoodsReceiptResponseDTO;

public interface GoodsReceiptService {

    GoodsReceiptResponseDTO receiveGoods(GoodsReceiptDTO dto);

    GoodsReceiptResponseDTO findById(Integer id);
}

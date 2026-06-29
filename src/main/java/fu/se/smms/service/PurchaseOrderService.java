package fu.se.smms.service;

import fu.se.smms.dto.PurchaseOrderDTO;
import fu.se.smms.dto.PurchaseOrderResponseDTO;
import fu.se.smms.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderService {

    PurchaseOrderResponseDTO create(PurchaseOrderDTO dto);

    PurchaseOrderResponseDTO findById(Integer id);

    Page<PurchaseOrderResponseDTO> search(Integer supplierId, OrderStatus status, Pageable pageable);

    PurchaseOrderResponseDTO updateStatus(Integer id, OrderStatus newStatus);
}

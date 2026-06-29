package fu.se.smms.service;

import fu.se.smms.dto.SupplierDTO;
import fu.se.smms.dto.SupplierResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    Page<SupplierResponseDTO> search(String keyword, Boolean status, Pageable pageable);

    SupplierResponseDTO findById(Integer id);

    SupplierResponseDTO findByIdWithPurchaseOrders(Integer id);

    SupplierResponseDTO create(SupplierDTO supplierDTO);

    SupplierResponseDTO update(Integer id, SupplierDTO supplierDTO);

    SupplierResponseDTO toggleStatus(Integer id);
}

package fu.se.smms.service;

import fu.se.smms.dto.SalesHistoryFilterReq;
import fu.se.smms.dto.SalesOrderResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SalesHistoryService {

    Page<SalesOrderResponseDTO> search(SalesHistoryFilterReq filter, Pageable pageable);

    SalesOrderResponseDTO findById(Integer id);
}

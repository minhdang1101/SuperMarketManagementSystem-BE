package fu.se.smms.service;

import fu.se.smms.dto.PromotionCreateReqDTO;
import fu.se.smms.dto.PromotionResponseDTO;
import fu.se.smms.dto.PromotionUpdateReqDTO;
import fu.se.smms.enums.ApplyTarget;
import fu.se.smms.enums.DiscountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface PromotionService {

    PromotionResponseDTO create(PromotionCreateReqDTO dto);

    PromotionResponseDTO findById(Integer id);

    Page<PromotionResponseDTO> search(String keyword, DiscountType discountType,
                                      ApplyTarget applyTarget, Boolean active, Pageable pageable);

    PromotionResponseDTO update(Integer id, PromotionUpdateReqDTO dto);

    void delete(Integer id);
}

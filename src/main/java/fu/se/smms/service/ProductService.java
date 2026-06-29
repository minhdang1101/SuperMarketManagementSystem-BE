package fu.se.smms.service;

import fu.se.smms.dto.ProductDTO;
import fu.se.smms.dto.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    Page<ProductResponseDTO> search(String keyword, Integer categoryId, Integer supplierId, Boolean status, Pageable pageable);

    ProductResponseDTO findById(Integer id);

    ProductResponseDTO findByBarcode(String barcode);

    ProductResponseDTO create(ProductDTO productDTO);

    ProductResponseDTO update(Integer id, ProductDTO productDTO);

    ProductResponseDTO uploadImages(Integer productId, List<MultipartFile> files);

    ProductResponseDTO deleteImage(Integer productId, int imageIndex);

    void deleteById(Integer id);
}

package fu.se.smms.service;

import fu.se.smms.dto.CheckoutRequestDTO;
import fu.se.smms.dto.CheckoutResponseDTO;
import fu.se.smms.dto.ProductSearchDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CheckoutService {

    Page<ProductSearchDTO> searchProducts(String query, Integer categoryId, Pageable pageable);
    

    ProductSearchDTO getProductByBarcode(String barcode);
    

    CheckoutResponseDTO calculateTotal(CheckoutRequestDTO request);
    

    CheckoutResponseDTO completeCheckout(CheckoutRequestDTO request, String cashierUsername);
}

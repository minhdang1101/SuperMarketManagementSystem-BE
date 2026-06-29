package fu.se.smms.controller;

import fu.se.smms.dto.ProductDTO;
import fu.se.smms.dto.ProductResponseDTO;
import fu.se.smms.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/products")
public class ProductController {
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer supplierId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        log.debug("Search products: keyword={}, categoryId={}, supplierId={}, status={}, page={}, size={}",
                keyword, categoryId, supplierId, status, page, size);
        return ResponseEntity.ok(productService.search(keyword, categoryId, supplierId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> findById(@PathVariable @Positive Integer id) {
        log.debug("Find product by id: {}", id);
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ProductResponseDTO> findByBarcode(@PathVariable String barcode) {
        log.debug("Find product by barcode: {}", barcode);
        return ResponseEntity.ok(productService.findByBarcode(barcode));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> create(@Valid @RequestBody ProductDTO productDTO) {
        log.info("Create product: {} (barcode: {})", productDTO.getName(), productDTO.getBarcode());
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(productDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody ProductDTO productDTO) {
        log.info("Update product id: {}, name: {}", id, productDTO.getName());
        return ResponseEntity.ok(productService.update(id, productDTO));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponseDTO> uploadImages(
            @PathVariable @Positive Integer id,
            @RequestParam("files") List<MultipartFile> files) {
        log.info("Upload {} images for product id: {}", files.size(), id);
        return ResponseEntity.ok(productService.uploadImages(id, files));
    }

    @DeleteMapping("/{id}/images/{imageIndex}")
    public ResponseEntity<ProductResponseDTO> deleteImage(
            @PathVariable @Positive Integer id,
            @PathVariable int imageIndex) {
        log.info("Delete image index {} for product id: {}", imageIndex, id);
        return ResponseEntity.ok(productService.deleteImage(id, imageIndex));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable @Positive Integer id) {
        log.info("Delete product id: {}", id);
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

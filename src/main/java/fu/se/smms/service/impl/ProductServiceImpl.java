package fu.se.smms.service.impl;

import fu.se.smms.dto.ProductDTO;
import fu.se.smms.dto.ProductResponseDTO;
import fu.se.smms.entity.Category;
import fu.se.smms.entity.Product;
import fu.se.smms.entity.Supplier;
import fu.se.smms.exception.BadRequestException;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.CategoryRepository;
import fu.se.smms.repository.ProductRepository;
import fu.se.smms.repository.SupplierRepository;
import fu.se.smms.service.FileStorageService;
import fu.se.smms.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final int MAX_IMAGES = 5;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final FileStorageService fileStorageService;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              SupplierRepository supplierRepository,
                              FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Page<ProductResponseDTO> search(String keyword, Integer categoryId, Integer supplierId,
                                           Boolean status, Pageable pageable) {
        boolean canViewInactive = hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER");

        if (!canViewInactive || status == null) {
            status = true;
        }

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return productRepository.search(trimmedKeyword, categoryId, supplierId, status, pageable)
                .map(this::toResponseDTO);
    }

    @Override
    public ProductResponseDTO findById(Integer id) {
        log.debug("Find product by id: {}", id);
        Product product = getProductOrThrow(id);
        return toResponseDTO(product);
    }

    @Override
    public ProductResponseDTO findByBarcode(String barcode) {
        log.debug("Find product by barcode: {}", barcode);
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> {
                    log.warn("Product not found with barcode: {}", barcode);
                    return new ResourceNotFoundException("Không tìm thấy sản phẩm với mã vạch: " + barcode);
                });
        return toResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO create(ProductDTO dto) {
        log.debug("Creating product: {} (barcode: {})", dto.getName(), dto.getBarcode());

        validateBarcodeUnique(dto.getBarcode(), null);
        validateStockLevels(dto.getMinStockLevel(), dto.getMaxStockLevel(), dto.getStockLevel());

        Category category = getCategoryOrThrow(dto.getCategoryId());
        Supplier supplier = dto.getSupplierId() != null ? getSupplierOrThrow(dto.getSupplierId()) : null;

        Product product = Product.builder()
                .name(dto.getName())
                .barcode(dto.getBarcode())
                .description(dto.getDescription())
                .unit(dto.getUnit())
                .costPrice(dto.getCostPrice())
                .sellingPrice(dto.getSellingPrice())
                .stockLevel(dto.getStockLevel() != null ? dto.getStockLevel() : 0)
                .minStockLevel(dto.getMinStockLevel())
                .maxStockLevel(dto.getMaxStockLevel())
                .images(dto.getImages() != null ? new ArrayList<>(dto.getImages()) : new ArrayList<>())
                .category(category)
                .supplier(supplier)
                .status(dto.getStatus() != null ? dto.getStatus() : true)
                .build();

        product = productRepository.save(product);
        log.info("Product created: id={}, barcode={}", product.getProductId(), product.getBarcode());
        return toResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO update(Integer id, ProductDTO dto) {
        log.debug("Updating product id: {}", id);
        Product product = getProductOrThrow(id);

        validateBarcodeUnique(dto.getBarcode(), id);
        validateStockLevels(dto.getMinStockLevel(), dto.getMaxStockLevel(), dto.getStockLevel());

        Category category = getCategoryOrThrow(dto.getCategoryId());
        Supplier supplier = dto.getSupplierId() != null ? getSupplierOrThrow(dto.getSupplierId()) : null;

        product.setName(dto.getName());
        product.setBarcode(dto.getBarcode());
        product.setDescription(dto.getDescription());
        product.setUnit(dto.getUnit());
        product.setCostPrice(dto.getCostPrice());
        product.setSellingPrice(dto.getSellingPrice());
        product.setStockLevel(dto.getStockLevel() != null ? dto.getStockLevel() : 0);
        product.setMinStockLevel(dto.getMinStockLevel());
        product.setMaxStockLevel(dto.getMaxStockLevel());
        product.setCategory(category);
        product.setSupplier(supplier);
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }

        product = productRepository.save(product);
        log.info("Product updated: id={}", product.getProductId());
        return toResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO uploadImages(Integer productId, List<MultipartFile> files) {
        log.debug("Uploading {} images for product id: {}", files.size(), productId);
        Product product = getProductOrThrow(productId);

        int currentCount = product.getImages().size();
        if (currentCount + files.size() > MAX_IMAGES) {
            throw new BadRequestException(
                    "Tối đa " + MAX_IMAGES + " hình ảnh. Hiện có " + currentCount + ", đang upload " + files.size());
        }

        List<String> newUrls = fileStorageService.storeFiles(files, "products/" + productId);
        product.getImages().addAll(newUrls);
        product = productRepository.save(product);

        log.info("Uploaded {} images for product id: {}", newUrls.size(), productId);
        return toResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO deleteImage(Integer productId, int imageIndex) {
        log.debug("Deleting image index {} for product id: {}", imageIndex, productId);
        Product product = getProductOrThrow(productId);

        if (imageIndex < 0 || imageIndex >= product.getImages().size()) {
            throw new BadRequestException("Chỉ số hình ảnh không hợp lệ: " + imageIndex);
        }

        String imageUrl = product.getImages().remove(imageIndex);
        fileStorageService.deleteFile(imageUrl);
        product = productRepository.save(product);

        log.info("Deleted image index {} for product id: {}", imageIndex, productId);
        return toResponseDTO(product);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        Product product = getProductOrThrow(id);
        if (!Boolean.TRUE.equals(product.getStatus())) {
            throw new BadRequestException("Sản phẩm đã bị vô hiệu hóa: " + id);
        }

        List<String> images = product.getImages();
        int imageCount = images.size();
        for (String imageUrl : images) {
            fileStorageService.deleteFile(imageUrl);
        }
        images.clear();

        product.setStatus(false);
        productRepository.save(product);
        log.info("Soft-deleted product id: {}, cleaned up {} images", id, imageCount);
    }

    private boolean hasAnyAuthority(String... authorities) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(ga -> {
                    for (String authority : authorities) {
                        if (authority.equals(ga.getAuthority())) return true;
                    }
                    return false;
                });
    }

    private Product getProductOrThrow(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
    }

    private Category getCategoryOrThrow(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục: " + categoryId));
    }

    private Supplier getSupplierOrThrow(Integer supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp: " + supplierId));
    }

    private void validateStockLevels(Integer minStockLevel, Integer maxStockLevel, Integer stockLevel) {
        if (minStockLevel != null && maxStockLevel != null && minStockLevel > maxStockLevel) {
            throw new BadRequestException("Mức tồn kho tối thiểu không được lớn hơn mức tối đa");
        }
        if (stockLevel != null && minStockLevel != null && stockLevel < minStockLevel) {
            throw new BadRequestException("Số lượng tồn kho không được nhỏ hơn mức tối thiểu: " + minStockLevel);
        }
        if (stockLevel != null && maxStockLevel != null && stockLevel > maxStockLevel) {
            throw new BadRequestException("Số lượng tồn kho không được lớn hơn mức tối đa: " + maxStockLevel);
        }
    }

    private void validateBarcodeUnique(String barcode, Integer excludeProductId) {
        if (excludeProductId == null) {
            if (productRepository.existsByBarcode(barcode)) {
                log.warn("Duplicate barcode on create: {}", barcode);
                throw new BadRequestException("Mã vạch đã tồn tại: " + barcode);
            }
        } else {
            if (productRepository.existsByBarcodeAndProductIdNot(barcode, excludeProductId)) {
                log.warn("Duplicate barcode on update: {} (productId: {})", barcode, excludeProductId);
                throw new BadRequestException("Mã vạch đã tồn tại: " + barcode);
            }
        }
    }

    private ProductResponseDTO toResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .productId(product.getProductId())
                .name(product.getName())
                .barcode(product.getBarcode())
                .description(product.getDescription())
                .unit(product.getUnit())
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .stockLevel(product.getStockLevel())
                .minStockLevel(product.getMinStockLevel())
                .maxStockLevel(product.getMaxStockLevel())
                .images(product.getImages() != null ? new ArrayList<>(product.getImages()) : new ArrayList<>())
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .supplierId(product.getSupplier() != null ? product.getSupplier().getSupplierId() : null)
                .supplierName(product.getSupplier() != null ? product.getSupplier().getName() : null)
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

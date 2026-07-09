package fu.se.smms.service.impl;

import fu.se.smms.dto.PromotionCreateReqDTO;
import fu.se.smms.dto.PromotionResponseDTO;
import fu.se.smms.dto.PromotionUpdateReqDTO;
import fu.se.smms.entity.Category;
import fu.se.smms.entity.Product;
import fu.se.smms.entity.Promotion;
import fu.se.smms.enums.ApplyTarget;
import fu.se.smms.enums.DiscountType;
import fu.se.smms.exception.InvalidPromotionRuleException;
import fu.se.smms.exception.PromotionOverlapException;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.CategoryRepository;
import fu.se.smms.repository.ProductRepository;
import fu.se.smms.repository.PromotionRepository;
import fu.se.smms.service.PromotionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PromotionServiceImpl implements PromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionServiceImpl.class);
    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("50");

    private final PromotionRepository promotionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public PromotionServiceImpl(PromotionRepository promotionRepository,
                                CategoryRepository categoryRepository,
                                ProductRepository productRepository) {
        this.promotionRepository = promotionRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public PromotionResponseDTO create(PromotionCreateReqDTO dto) {
        log.info("Tạo mới Promotion: name={}, type={}", dto.getName(), dto.getDiscountType());

        String name = dto.getName().trim();
        String description = dto.getDescription() != null ? dto.getDescription().trim() : null;

        validateDates(dto.getValidFrom(), dto.getValidTo());
        validateDiscountValue(dto.getDiscountType(), dto.getDiscountValue());
        validateTypeSpecificFields(dto);

        Category category = null;
        List<Product> targetProducts = new ArrayList<>();
        if (dto.getApplyTarget() == ApplyTarget.CATEGORY) {
            category = validateAndGetCategory(dto.getCategoryId());
        } else {
            targetProducts = validateAndGetProducts(dto.getProductIds());
        }

        checkOverlapping(dto);

        Product getProduct = null;
        if (dto.getDiscountType() == DiscountType.BUY_X_GET_Y) {
            getProduct = validateBuyXGetYPricing(dto, category, targetProducts);
        }

        String promoCode = generatePromoCode();
        log.info("Generated promoCode: {}", promoCode);

        Promotion promotion = Promotion.builder()
                .promoCode(promoCode)
                .name(name)
                .description(description)
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .applyTarget(dto.getApplyTarget())
                .buyQuantity(dto.getBuyQuantity())
                .getQuantity(dto.getGetQuantity())
                .getProduct(getProduct)
                .minOrderAmount(dto.getMinOrderAmount())
                .startDate(dto.getValidFrom())
                .endDate(dto.getValidTo())
                .active(true)
                .category(category)
                .products(targetProducts)
                .build();

        promotion = promotionRepository.save(promotion);
        log.info("Promotion created: id={}, promoCode={}", promotion.getPromotionId(), promoCode);

        return toResponseDTO(promotion);
    }

    @Override
    public PromotionResponseDTO findById(Integer id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MSG01 - Không tìm thấy Promotion với id: " + id));
        return toResponseDTO(promotion);
    }

    @Override
    public Page<PromotionResponseDTO> search(String keyword, DiscountType discountType,
                                             ApplyTarget applyTarget, Boolean active, Pageable pageable) {
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return promotionRepository.search(trimmedKeyword, discountType, applyTarget, active, pageable)
                .map(this::toResponseDTO);
    }

    @Override
    @Transactional
    public PromotionResponseDTO update(Integer id, PromotionUpdateReqDTO dto) {
        log.info("Cập nhật Promotion id={}, name={}", id, dto.getName());

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MSG01 - Không tìm thấy Promotion với id: " + id));

        if (dto.getValidTo().isBefore(dto.getValidFrom())) {
            throw new InvalidPromotionRuleException("MSG03",
                    "BR-04: Ngày kết thúc (" + dto.getValidTo() + ") phải >= ngày bắt đầu (" + dto.getValidFrom() + ")");
        }

        validateDiscountValue(dto.getDiscountType(), dto.getDiscountValue());

        if (dto.getDiscountType() == DiscountType.BUY_X_GET_Y) {
            if (dto.getBuyQuantity() == null || dto.getGetQuantity() == null || dto.getGetProductId() == null) {
                throw new InvalidPromotionRuleException("MSG05",
                        "BUY_X_GET_Y: Các trường buyQuantity, getQuantity, getProductId là bắt buộc");
            }
        }

        promotion.setName(dto.getName().trim());
        promotion.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        promotion.setDiscountType(dto.getDiscountType());
        promotion.setDiscountValue(dto.getDiscountValue());
        promotion.setApplyTarget(dto.getApplyTarget());
        promotion.setStartDate(dto.getValidFrom());
        promotion.setEndDate(dto.getValidTo());
        promotion.setMinOrderAmount(dto.getMinOrderAmount());

        if (dto.getActive() != null) {
            promotion.setActive(dto.getActive());
        }

        if (dto.getApplyTarget() == ApplyTarget.CATEGORY) {
            Category category = validateAndGetCategory(dto.getCategoryId());
            promotion.setCategory(category);
            promotion.setProducts(new ArrayList<>());
        } else {
            List<Product> products = validateAndGetProducts(dto.getProductIds());
            promotion.setProducts(products);
            promotion.setCategory(null);
        }

        if (dto.getDiscountType() == DiscountType.BUY_X_GET_Y) {
            promotion.setBuyQuantity(dto.getBuyQuantity());
            promotion.setGetQuantity(dto.getGetQuantity());
            Product getProduct = productRepository.findById(dto.getGetProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "MSG01 - Không tìm thấy sản phẩm tặng: " + dto.getGetProductId()));
            promotion.setGetProduct(getProduct);
        } else {
            promotion.setBuyQuantity(null);
            promotion.setGetQuantity(null);
            promotion.setGetProduct(null);
        }

        promotion = promotionRepository.save(promotion);
        log.info("Promotion updated successfully: id={}", id);

        return toResponseDTO(promotion);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        log.info("Xóa Promotion id={}", id);

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MSG01 - Không tìm thấy Promotion với id: " + id));

        promotion.setActive(false);
        promotionRepository.save(promotion);

        log.info("Promotion soft-deleted: id={}", id);
    }

    private void validateDates(LocalDate validFrom, LocalDate validTo) {
        if (validFrom.isBefore(LocalDate.now())) {
            throw new InvalidPromotionRuleException("MSG03",
                    "BR-04: Ngày bắt đầu (" + validFrom + ") không được nằm trong quá khứ");
        }
        if (validTo.isBefore(validFrom)) {
            throw new InvalidPromotionRuleException("MSG03",
                    "BR-04: Ngày kết thúc (" + validTo + ") phải lớn hơn hoặc bằng ngày bắt đầu (" + validFrom + ")");
        }
    }

    private void validateDiscountValue(DiscountType type, BigDecimal discountValue) {
        if (type == DiscountType.PERCENTAGE && discountValue.compareTo(MAX_PERCENTAGE) > 0) {
            throw new InvalidPromotionRuleException("MSG03",
                    "BR-03: Giá trị giảm theo phần trăm không được vượt quá 50%. Giá trị nhập: " + discountValue + "%");
        }
    }

    private void validateTypeSpecificFields(PromotionCreateReqDTO dto) {
        if (dto.getDiscountType() == DiscountType.BUY_X_GET_Y) {
            if (dto.getBuyQuantity() == null) {
                throw new InvalidPromotionRuleException("MSG05",
                        "BUY_X_GET_Y: Trường buyQuantity là bắt buộc");
            }
            if (dto.getGetQuantity() == null) {
                throw new InvalidPromotionRuleException("MSG05",
                        "BUY_X_GET_Y: Trường getQuantity là bắt buộc");
            }
            if (dto.getGetProductId() == null) {
                throw new InvalidPromotionRuleException("MSG05",
                        "BUY_X_GET_Y: Trường getProductId là bắt buộc");
            }
        }
    }

    private Category validateAndGetCategory(Integer categoryId) {
        if (categoryId == null) {
            throw new InvalidPromotionRuleException("MSG05",
                    "ApplyTarget = CATEGORY: Trường categoryId là bắt buộc");
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("MSG01 - Không tìm thấy danh mục: " + categoryId));
    }

    private List<Product> validateAndGetProducts(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new InvalidPromotionRuleException("MSG05",
                    "ApplyTarget = PRODUCT: Trường productIds là bắt buộc và không được rỗng");
        }
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            List<Integer> foundIds = products.stream().map(Product::getProductId).toList();
            List<Integer> missingIds = productIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new ResourceNotFoundException("MSG01 - Không tìm thấy sản phẩm với ID: " + missingIds);
        }
        return products;
    }

    private void checkOverlapping(PromotionCreateReqDTO dto) {
        List<Promotion> overlapping;

        if (dto.getApplyTarget() == ApplyTarget.CATEGORY) {
            overlapping = promotionRepository.findOverlappingByCategory(
                    dto.getCategoryId(), dto.getValidFrom(), dto.getValidTo());
        } else {
            overlapping = promotionRepository.findOverlappingByProducts(
                    dto.getProductIds(), dto.getValidFrom(), dto.getValidTo());
        }

        if (!overlapping.isEmpty()) {
            String conflictCodes = overlapping.stream()
                    .map(p -> p.getPromoCode() + " (" + p.getStartDate() + " → " + p.getEndDate() + ")")
                    .collect(Collectors.joining(", "));
            throw new PromotionOverlapException("MSG06",
                    "AT2: Phát hiện Promotion trùng lặp thời gian cho cùng đối tượng áp dụng. "
                            + "Promotion xung đột: [" + conflictCodes + "]");
        }
    }

    private Product validateBuyXGetYPricing(PromotionCreateReqDTO dto, Category category, List<Product> targetProducts) {
        Product getProduct = productRepository.findById(dto.getGetProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MSG01 - Không tìm thấy sản phẩm tặng với ID: " + dto.getGetProductId()));

        List<Product> buyProducts;
        if (dto.getApplyTarget() == ApplyTarget.CATEGORY && category != null) {
            buyProducts = productRepository.findAllById(
                    category.getProducts().stream().map(Product::getProductId).toList());
        } else {
            buyProducts = targetProducts;
        }

        if (!buyProducts.isEmpty()) {
            BigDecimal minBuyPrice = buyProducts.stream()
                    .map(Product::getSellingPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            if (getProduct.getSellingPrice().compareTo(minBuyPrice) > 0) {
                throw new InvalidPromotionRuleException("MSG03",
                        "BR-05: Giá sản phẩm tặng (" + getProduct.getSellingPrice()
                                + ") phải <= giá thấp nhất của sản phẩm mua (" + minBuyPrice + ")");
            }
        }

        return getProduct;
    }

    private String generatePromoCode() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String prefix = "PROMO-" + yearMonth + "-";

        String lastCode = promotionRepository.findLastPromoCodeByPrefix(prefix).orElse(null);

        int nextSequence = 1;
        if (lastCode != null) {
            String seqStr = lastCode.substring(lastCode.lastIndexOf('-') + 1);
            nextSequence = Integer.parseInt(seqStr) + 1;
        }

        return prefix + String.format("%03d", nextSequence);
    }

    private PromotionResponseDTO toResponseDTO(Promotion p) {
        return PromotionResponseDTO.builder()
                .promotionId(p.getPromotionId())
                .promoCode(p.getPromoCode())
                .name(p.getName())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .applyTarget(p.getApplyTarget())
                .categoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .productIds(p.getProducts() != null
                        ? p.getProducts().stream().map(Product::getProductId).toList()
                        : null)
                .productNames(p.getProducts() != null
                        ? p.getProducts().stream().map(Product::getName).toList()
                        : null)
                .buyQuantity(p.getBuyQuantity())
                .getQuantity(p.getGetQuantity())
                .getProductId(p.getGetProduct() != null ? p.getGetProduct().getProductId() : null)
                .getProductName(p.getGetProduct() != null ? p.getGetProduct().getName() : null)
                .minOrderAmount(p.getMinOrderAmount())
                .validFrom(p.getStartDate())
                .validTo(p.getEndDate())
                .active(p.getActive())
                .currentlyActive(p.isCurrentlyActive())
                .createdAt(p.getCreatedAt())
                .createdBy(p.getCreatedBy())
                .updatedAt(p.getUpdatedAt())
                .lastModifiedBy(p.getLastModifiedBy())
                .build();
    }
}

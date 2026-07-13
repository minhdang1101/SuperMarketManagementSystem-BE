package fu.se.smms.service.impl;

import fu.se.smms.dto.*;
import fu.se.smms.dto.CheckoutRequestDTO.CartItem;
import fu.se.smms.dto.CheckoutResponseDTO.CheckoutItemDTO;
import fu.se.smms.entity.*;
import fu.se.smms.enums.PaymentMethod;
import fu.se.smms.enums.PaymentStatus;
import fu.se.smms.exception.InsufficientStockException;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.*;
import fu.se.smms.service.CheckoutService;
import fu.se.smms.service.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class CheckoutServiceImpl implements CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutServiceImpl.class);
    private static final BigDecimal DEFAULT_VAT_RATE = BigDecimal.valueOf(10);
    private static final int SCALE = 4;

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SystemSettingService systemSettingService;

    public CheckoutServiceImpl(ProductRepository productRepository,
                               PromotionRepository promotionRepository,
                               CustomerRepository customerRepository,
                               UserRepository userRepository,
                               SalesOrderRepository salesOrderRepository,
                               SystemSettingService systemSettingService) {
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.systemSettingService = systemSettingService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSearchDTO> searchProducts(String query, Integer categoryId, Pageable pageable) {
        log.debug("Searching products for POS: query={}, categoryId={}", query, categoryId);
        
        Page<Product> products = productRepository.searchForPOS(query, categoryId, pageable);
        LocalDate today = LocalDate.now();
        
        return products.map(p -> {
            List<Promotion> activePromos = promotionRepository.findActivePromotionsForProduct(
                p.getProductId(),
                p.getCategory() != null ? p.getCategory().getCategoryId() : null,
                today
            );
            
            String promoBadge = null;
            boolean hasPromotion = !activePromos.isEmpty();
            
            if (hasPromotion) {
                Promotion promo = activePromos.get(0);
                promoBadge = formatPromotionBadge(promo);
            }
            
            return ProductSearchDTO.builder()
                    .productId(p.getProductId())
                    .barcode(p.getBarcode())
                    .name(p.getName())
                    .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                    .sellingPrice(p.getSellingPrice())
                    .stockLevel(p.getStockLevel())
                    .imageUrl(p.getImages() != null && !p.getImages().isEmpty() ? p.getImages().get(0) : null)
                    .hasPromotion(hasPromotion)
                    .promotionBadge(promoBadge)
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSearchDTO getProductByBarcode(String barcode) {
        log.debug("Getting product by barcode: {}", barcode);
        
        Product product = productRepository.findByBarcodeIgnoreCase(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "MSG01 - Không tìm thấy sản phẩm với mã vạch: " + barcode));
        
        LocalDate today = LocalDate.now();
        List<Promotion> activePromos = promotionRepository.findActivePromotionsForProduct(
            product.getProductId(),
            product.getCategory() != null ? product.getCategory().getCategoryId() : null,
            today
        );
        
        String promoBadge = null;
        boolean hasPromotion = !activePromos.isEmpty();
        
        if (hasPromotion) {
            promoBadge = formatPromotionBadge(activePromos.get(0));
        }
        
        return ProductSearchDTO.builder()
                .productId(product.getProductId())
                .barcode(product.getBarcode())
                .name(product.getName())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .sellingPrice(product.getSellingPrice())
                .stockLevel(product.getStockLevel())
                .imageUrl(product.getImages() != null && !product.getImages().isEmpty() ? product.getImages().get(0) : null)
                .hasPromotion(hasPromotion)
                .promotionBadge(promoBadge)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutResponseDTO calculateTotal(CheckoutRequestDTO request) {
        log.debug("Calculating cart total for {} items", request.getItems().size());
        

        List<Integer> productIds = request.getItems().stream()
                .map(CartItem::getProductId)
                .toList();
        
        Map<Integer, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
        

        for (CartItem item : request.getItems()) {
            if (!productMap.containsKey(item.getProductId())) {
                throw new ResourceNotFoundException(
                    "MSG01 - Không tìm thấy sản phẩm ID: " + item.getProductId());
            }
        }
        

        List<CheckoutItemDTO> checkoutItems = buildCheckoutItems(request.getItems(), productMap);

        BigDecimal subtotal = checkoutItems.stream()
                .map(CheckoutItemDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Promotion appliedPromo = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountDescription = null;
        
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            appliedPromo = promotionRepository.findByPromoCodeAndActiveTrue(request.getPromoCode())
                    .orElse(null);
            
            if (appliedPromo != null && isPromotionApplicable(appliedPromo, subtotal)) {
                discountAmount = calculatePromotionDiscount(appliedPromo, checkoutItems, productMap);
                discountDescription = appliedPromo.getName();
            }
        }

        BigDecimal vatRate = getVatRate();
        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxableAmount.multiply(vatRate.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP));

        BigDecimal totalAmount = taxableAmount.add(taxAmount);

        BigDecimal receivedAmount = request.getReceivedAmount() != null 
                ? request.getReceivedAmount() 
                : BigDecimal.ZERO;
        BigDecimal changeAmount = receivedAmount.subtract(totalAmount).max(BigDecimal.ZERO);

        String customerName = "Khách lẻ";
        Integer loyaltyPoints = null;
        
        if (request.getMemberCardId() != null && !request.getMemberCardId().isBlank()) {
            Customer customer = customerRepository.findByMemberCardId(request.getMemberCardId())
                    .orElse(null);
            if (customer != null) {
                customerName = customer.getName();
                loyaltyPoints = customer.getPoints();
            }
        }
        
        return CheckoutResponseDTO.builder()
                .items(checkoutItems)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .discountDescription(discountDescription)
                .taxRate(vatRate)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .receivedAmount(receivedAmount)
                .changeAmount(changeAmount)
                .customerName(customerName)
                .memberCardId(request.getMemberCardId())
                .loyaltyPoints(loyaltyPoints)
                .promotionCode(appliedPromo != null ? appliedPromo.getPromoCode() : null)
                .promotionName(appliedPromo != null ? appliedPromo.getName() : null)
                .build();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public CheckoutResponseDTO completeCheckout(CheckoutRequestDTO request, String cashierUsername) {
        log.info("Processing checkout for cashier: {}", cashierUsername);

        User cashier = userRepository.findByUsername(cashierUsername)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "MSG01 - Không tìm thấy nhân viên: " + cashierUsername));

        List<Integer> productIds = request.getItems().stream()
                .map(CartItem::getProductId)
                .toList();

        List<Product> products = productRepository.findAllByIdWithLock(productIds);
        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        for (CartItem item : request.getItems()) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException(
                    "MSG01 - Không tìm thấy sản phẩm ID: " + item.getProductId());
            }
            
            if (product.getStockLevel() < item.getQuantity()) {
                throw new InsufficientStockException(
                    "MSG07",
                    "Sản phẩm '" + product.getName() + "' không đủ tồn kho. Yêu cầu: " 
                        + item.getQuantity() + ", Còn: " + product.getStockLevel(),
                    product.getProductId(),
                    item.getQuantity(),
                    product.getStockLevel()
                );
            }
        }

        List<CheckoutItemDTO> checkoutItems = buildCheckoutItems(request.getItems(), productMap);

        BigDecimal subtotal = checkoutItems.stream()
                .map(CheckoutItemDTO::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Promotion appliedPromo = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            appliedPromo = promotionRepository.findByPromoCodeAndActiveTrue(request.getPromoCode())
                    .orElse(null);
            
            if (appliedPromo != null && isPromotionApplicable(appliedPromo, subtotal)) {
                discountAmount = calculatePromotionDiscount(appliedPromo, checkoutItems, productMap);
            }
        }

        BigDecimal vatRate = getVatRate();
        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxableAmount.multiply(vatRate.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP));

        BigDecimal totalAmount = taxableAmount.add(taxAmount);

        BigDecimal receivedAmount = request.getReceivedAmount() != null 
                ? request.getReceivedAmount() 
                : totalAmount;
        BigDecimal changeAmount = BigDecimal.ZERO;
        
        if (request.getPaymentMethod() == PaymentMethod.CASH) {
            if (receivedAmount.compareTo(totalAmount) < 0) {
                throw new InsufficientStockException("MSG08",
                    "Số tiền nhận (" + receivedAmount + ") không đủ thanh toán (" + totalAmount + ")");
            }
            changeAmount = receivedAmount.subtract(totalAmount);
        } else {
            receivedAmount = totalAmount;
        }
        
        // Get customer
        Customer customer = null;
        if (request.getMemberCardId() != null && !request.getMemberCardId().isBlank()) {
            customer = customerRepository.findByMemberCardId(request.getMemberCardId())
                    .orElse(null);
        }

        String invoiceNumber = generateInvoiceNumber();

        boolean isVnPay = request.getPaymentMethod() == PaymentMethod.VNPAY;

        SalesOrder salesOrder = SalesOrder.builder()
                .invoiceNumber(invoiceNumber)
                .orderDate(LocalDateTime.now())
                .cashier(cashier)
                .customer(customer)
                .promotion(appliedPromo)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .receivedAmount(receivedAmount)
                .changeAmount(changeAmount)
                // VNPAY stays PENDING until IPN confirms; CASH/CARD are immediately PAID
                .paymentStatus(isVnPay ? PaymentStatus.PENDING : PaymentStatus.PAID)
                .build();

        for (CartItem item : request.getItems()) {
            Product product = productMap.get(item.getProductId());
            
            SalesOrderDetail detail = SalesOrderDetail.builder()
                    .salesOrder(salesOrder)
                    .product(product)
                    .quantity(item.getQuantity())
                    .unitPrice(product.getSellingPrice())
                    .costPrice(product.getCostPrice())
                    .discountAmount(BigDecimal.ZERO)
                    .totalPrice(product.getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();
            
            salesOrder.getSalesOrderDetails().add(detail);

            // For VNPAY: stock is deducted only after payment confirmed via IPN
            if (!isVnPay) {
                int updated = productRepository.decreaseStockLevel(product.getProductId(), item.getQuantity());
                if (updated == 0) {
                    throw new InsufficientStockException(
                        "MSG07",
                        "Không thể cập nhật tồn kho sản phẩm: " + product.getName(),
                        product.getProductId(),
                        item.getQuantity(),
                        product.getStockLevel()
                    );
                }
            }
        }

        salesOrder = salesOrderRepository.save(salesOrder);
        log.info("Order created: invoiceNumber={}, total={}", invoiceNumber, totalAmount);

        if (customer != null) {
            int pointsEarned = totalAmount.divide(BigDecimal.valueOf(1000), 0, RoundingMode.DOWN).intValue();
            customer.setPoints(customer.getPoints() + pointsEarned);
        }
        
        return CheckoutResponseDTO.builder()
                .orderId(salesOrder.getSalesOrderId())
                .invoiceNumber(invoiceNumber)
                .orderDate(salesOrder.getOrderDate())
                .cashierName(cashier.getName())
                .customerName(customer != null ? customer.getName() : "Khách lẻ")
                .memberCardId(request.getMemberCardId())
                .loyaltyPoints(customer != null ? customer.getPoints() : null)
                .items(checkoutItems)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .discountDescription(appliedPromo != null ? appliedPromo.getName() : null)
                .taxRate(vatRate)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .receivedAmount(receivedAmount)
                .changeAmount(changeAmount)
                .promotionCode(appliedPromo != null ? appliedPromo.getPromoCode() : null)
                .promotionName(appliedPromo != null ? appliedPromo.getName() : null)
                .build();
    }

    private List<CheckoutItemDTO> buildCheckoutItems(List<CartItem> items, Map<Integer, Product> productMap) {
        return items.stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    BigDecimal lineTotal = product.getSellingPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    
                    return CheckoutItemDTO.builder()
                            .productId(product.getProductId())
                            .barcode(product.getBarcode())
                            .productName(product.getName())
                            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                            .unitPrice(product.getSellingPrice())
                            .quantity(item.getQuantity())
                            .lineTotal(lineTotal)
                            .discountAmount(BigDecimal.ZERO)
                            .imageUrl(product.getImages() != null && !product.getImages().isEmpty() ? product.getImages().get(0) : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private boolean isPromotionApplicable(Promotion promo, BigDecimal subtotal) {
        LocalDate today = LocalDate.now();
        
        if (!promo.getActive()) return false;
        if (today.isBefore(promo.getStartDate()) || today.isAfter(promo.getEndDate())) return false;
        if (promo.getMinOrderAmount() != null && subtotal.compareTo(promo.getMinOrderAmount()) < 0) return false;
        
        return true;
    }

    private BigDecimal calculatePromotionDiscount(Promotion promo, List<CheckoutItemDTO> items, 
            Map<Integer, Product> productMap) {
        
        BigDecimal discount = BigDecimal.ZERO;
        
        switch (promo.getDiscountType()) {
            case PERCENTAGE -> {
                BigDecimal applicableAmount = calculateApplicableAmount(promo, items, productMap);
                discount = applicableAmount.multiply(promo.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
            }
            case FIXED_AMOUNT -> {
                discount = promo.getDiscountValue();
            }
            case BUY_X_GET_Y -> {
                // Calculate how many free items customer gets
                if (promo.getBuyQuantity() != null && promo.getGetQuantity() != null 
                        && promo.getGetProduct() != null) {
                    int totalQualifyingItems = getQualifyingItemCount(promo, items, productMap);
                    int freeItemSets = totalQualifyingItems / promo.getBuyQuantity();
                    int freeItems = freeItemSets * promo.getGetQuantity();
                    
                    if (freeItems > 0) {
                        discount = promo.getGetProduct().getSellingPrice()
                                .multiply(BigDecimal.valueOf(freeItems));
                    }
                }
            }
        }
        
        return discount;
    }

    private BigDecimal calculateApplicableAmount(Promotion promo, List<CheckoutItemDTO> items,
            Map<Integer, Product> productMap) {
        
        if (promo.getApplyTarget() == null) {
            return items.stream()
                    .map(CheckoutItemDTO::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        return switch (promo.getApplyTarget()) {
            case CATEGORY -> {
                Integer categoryId = promo.getCategory() != null ? promo.getCategory().getCategoryId() : null;
                yield items.stream()
                        .filter(item -> {
                            Product p = productMap.get(item.getProductId());
                            return p != null && p.getCategory() != null 
                                    && p.getCategory().getCategoryId().equals(categoryId);
                        })
                        .map(CheckoutItemDTO::getLineTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            case PRODUCT -> {
                Set<Integer> promoProductIds = promo.getProducts() != null 
                        ? promo.getProducts().stream().map(Product::getProductId).collect(Collectors.toSet())
                        : Collections.emptySet();
                yield items.stream()
                        .filter(item -> promoProductIds.contains(item.getProductId()))
                        .map(CheckoutItemDTO::getLineTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        };
    }

    private int getQualifyingItemCount(Promotion promo, List<CheckoutItemDTO> items,
            Map<Integer, Product> productMap) {
        
        if (promo.getApplyTarget() == null) {
            return items.stream().mapToInt(CheckoutItemDTO::getQuantity).sum();
        }
        
        return switch (promo.getApplyTarget()) {
            case CATEGORY -> {
                Integer categoryId = promo.getCategory() != null ? promo.getCategory().getCategoryId() : null;
                yield items.stream()
                        .filter(item -> {
                            Product p = productMap.get(item.getProductId());
                            return p != null && p.getCategory() != null 
                                    && p.getCategory().getCategoryId().equals(categoryId);
                        })
                        .mapToInt(CheckoutItemDTO::getQuantity)
                        .sum();
            }
            case PRODUCT -> {
                Set<Integer> promoProductIds = promo.getProducts() != null 
                        ? promo.getProducts().stream().map(Product::getProductId).collect(Collectors.toSet())
                        : Collections.emptySet();
                yield items.stream()
                        .filter(item -> promoProductIds.contains(item.getProductId()))
                        .mapToInt(CheckoutItemDTO::getQuantity)
                        .sum();
            }
        };
    }

    private String formatPromotionBadge(Promotion promo) {
        return switch (promo.getDiscountType()) {
            case PERCENTAGE -> "-" + promo.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
            case FIXED_AMOUNT -> "-" + formatMoney(promo.getDiscountValue());
            case BUY_X_GET_Y -> "Mua " + promo.getBuyQuantity() + " tặng " + promo.getGetQuantity();
        };
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,.0f₫", amount);
    }

    private BigDecimal getVatRate() {
        try {
            SystemSettingsDTO settings = systemSettingService.getSettings();
            if (settings != null && settings.getVatRate() != null) {
                return BigDecimal.valueOf(settings.getVatRate());
            }
        } catch (Exception e) {
            log.warn("Failed to get VAT rate from settings, using default: {}", DEFAULT_VAT_RATE);
        }
        return DEFAULT_VAT_RATE;
    }

    private synchronized String generateInvoiceNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "INV-" + datePrefix + "-";
        
        String lastInvoice = salesOrderRepository.findLastInvoiceNumberByPrefix(prefix).orElse(null);
        
        int nextSequence = 1;
        if (lastInvoice != null) {
            String seqStr = lastInvoice.substring(lastInvoice.lastIndexOf('-') + 1);
            nextSequence = Integer.parseInt(seqStr) + 1;
        }
        
        return prefix + String.format("%04d", nextSequence);
    }
}

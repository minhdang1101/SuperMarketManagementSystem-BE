package fu.se.smms.repository;

import fu.se.smms.entity.Promotion;
import fu.se.smms.enums.ApplyTarget;
import fu.se.smms.enums.DiscountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    @Query("SELECT MAX(p.promoCode) FROM Promotion p WHERE p.promoCode LIKE CONCAT(:prefix, '%')")
    Optional<String> findLastPromoCodeByPrefix(@Param("prefix") String prefix);

    @Query("""
            SELECT p FROM Promotion p
            WHERE p.category.categoryId = :categoryId
              AND p.startDate <= :endDate
              AND p.endDate >= :startDate
              AND p.active = true
            """)
    List<Promotion> findOverlappingByCategory(@Param("categoryId") Integer categoryId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT DISTINCT p FROM Promotion p
            JOIN p.products pp
            WHERE pp.productId IN :productIds
              AND p.startDate <= :endDate
              AND p.endDate >= :startDate
              AND p.active = true
            """)
    List<Promotion> findOverlappingByProducts(@Param("productIds") List<Integer> productIds,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query(value = """
            SELECT p FROM Promotion p
            LEFT JOIN FETCH p.category
            WHERE (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.promoCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:discountType IS NULL OR p.discountType = :discountType)
              AND (:applyTarget IS NULL OR p.applyTarget = :applyTarget)
              AND (:active IS NULL OR p.active = :active)
            """,
           countQuery = """
            SELECT COUNT(p) FROM Promotion p
            WHERE (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.promoCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:discountType IS NULL OR p.discountType = :discountType)
              AND (:applyTarget IS NULL OR p.applyTarget = :applyTarget)
              AND (:active IS NULL OR p.active = :active)
            """)
    Page<Promotion> search(@Param("keyword") String keyword,
                           @Param("discountType") DiscountType discountType,
                           @Param("applyTarget") ApplyTarget applyTarget,
                           @Param("active") Boolean active,
                           Pageable pageable);

    @Query("""
            SELECT p FROM Promotion p
            LEFT JOIN FETCH p.category
            LEFT JOIN FETCH p.products
            WHERE UPPER(p.promoCode) = UPPER(:promoCode) AND p.active = true
            """)
    Optional<Promotion> findByPromoCodeAndActiveTrue(@Param("promoCode") String promoCode);

    @Query("""
            SELECT DISTINCT p FROM Promotion p
            LEFT JOIN p.products pp
            WHERE p.active = true
              AND p.startDate <= :today AND p.endDate >= :today
              AND (
                  (p.applyTarget = fu.se.smms.enums.ApplyTarget.PRODUCT AND pp.productId = :productId)
                  OR (p.applyTarget = fu.se.smms.enums.ApplyTarget.CATEGORY AND p.category.categoryId = :categoryId)
              )
            """)
    List<Promotion> findActivePromotionsForProduct(@Param("productId") Integer productId,
                                                    @Param("categoryId") Integer categoryId,
                                                    @Param("today") LocalDate today);
}

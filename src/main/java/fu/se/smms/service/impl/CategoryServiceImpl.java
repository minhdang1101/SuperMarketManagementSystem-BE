package fu.se.smms.service.impl;

import fu.se.smms.dto.CategoryDTO;
import fu.se.smms.entity.Category;
import fu.se.smms.exception.BadRequestException;
import fu.se.smms.exception.ResourceNotFoundException;
import fu.se.smms.repository.CategoryRepository;
import fu.se.smms.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);
    private final CategoryRepository categoryRepository;
    private static final String CATEGORY_NOT_FOUND = "Category not found: ";
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryDTO> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public CategoryDTO findById(Integer id) {
        log.debug("Find category by id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found: {}", id);
                    return new ResourceNotFoundException(CATEGORY_NOT_FOUND + id);
                });
        return toDTO(category);
    }

    @Override
    public CategoryDTO create(CategoryDTO categoryDTO) {
        log.debug("Creating category: {}", categoryDTO.getName());
        if (categoryRepository.existsByName(categoryDTO.getName())) {
            throw new BadRequestException("Tên danh mục đã tồn tại: " + categoryDTO.getName());
        }
        Category category = Category.builder()
                .name(categoryDTO.getName())
                .description(categoryDTO.getDescription())
                .status(categoryDTO.getStatus() != null ? categoryDTO.getStatus() : true)
                .build();
        category = categoryRepository.save(category);
        return toDTO(category);
    }

    @Override
    public CategoryDTO update(Integer id, CategoryDTO categoryDTO) {
        log.debug("Updating category id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found for update: {}", id);
                    return new ResourceNotFoundException(CATEGORY_NOT_FOUND + id);
                });
        if (categoryRepository.existsByNameAndCategoryIdNot(categoryDTO.getName(), id)) {
            throw new BadRequestException("Tên danh mục đã tồn tại: " + categoryDTO.getName());
        }
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        if (categoryDTO.getStatus() != null) {
            category.setStatus(categoryDTO.getStatus());
        }
        category = categoryRepository.save(category);
        return toDTO(category);
    }

    @Override
    public void deleteById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found for delete: {}", id);
                    return new ResourceNotFoundException(CATEGORY_NOT_FOUND + id);
                });
        category.setStatus(false);
        categoryRepository.save(category);
        log.info("Soft-deleted category id: {} (had {} products)", id, category.getProducts().size());
    }

    @Override
    public List<CategoryDTO> search(String keyword) {
        log.debug("Search categories with keyword: {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return categoryRepository.findByNameContainingIgnoreCase(keyword.trim()).stream()
                .map(this::toDTO)
                .toList();
    }

    private CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .status(category.getStatus())
                .productCount(category.getProducts() != null ? category.getProducts().size() : 0)
                .build();
    }
}

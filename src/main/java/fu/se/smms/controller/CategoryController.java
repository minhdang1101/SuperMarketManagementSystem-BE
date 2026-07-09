package fu.se.smms.controller;

import fu.se.smms.dto.CategoryDTO;
import fu.se.smms.service.CategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/categories")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class CategoryController {
    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<CategoryDTO>> findAll() {
        log.debug("Find all categories");
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<List<CategoryDTO>> search(@RequestParam(required = false) String keyword) {
        log.debug("Search categories with keyword: {}", keyword);
        return ResponseEntity.ok(categoryService.search(keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
    public ResponseEntity<CategoryDTO> findById(@PathVariable @Positive Integer id) {
        log.debug("Find category by id: {}", id);
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> create(@Valid @RequestBody CategoryDTO categoryDTO) {
        log.info("Create category: {}", categoryDTO.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(categoryDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> update(@PathVariable @Positive Integer id, @Valid @RequestBody CategoryDTO categoryDTO) {
        log.info("Update category id: {}, name: {}", id, categoryDTO.getName());
        return ResponseEntity.ok(categoryService.update(id, categoryDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable @Positive Integer id) {
        log.info("Delete category id: {}", id);
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

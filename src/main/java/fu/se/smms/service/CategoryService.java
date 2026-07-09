package fu.se.smms.service;

import fu.se.smms.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {
    List<CategoryDTO> findAll();

    CategoryDTO findById(Integer id);

    CategoryDTO create(CategoryDTO categoryDTO);

    CategoryDTO update(Integer id, CategoryDTO categoryDTO);

    void deleteById(Integer id);

    List<CategoryDTO> search(String keyword);
}

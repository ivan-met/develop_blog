package met.ivan.devblog.service;

import met.ivan.devblog.dto.CategoryResponse;
import met.ivan.devblog.dto.CreateCategoryRequest;
import met.ivan.devblog.dto.UpdateCategoryRequest;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> list();

    CategoryResponse getBySlug(String slug);

    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse update(Long id, UpdateCategoryRequest request);

    void delete(Long id);
}

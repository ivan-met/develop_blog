package met.ivan.devblog.service.impl;

import met.ivan.devblog.dto.CategoryResponse;
import met.ivan.devblog.dto.CreateCategoryRequest;
import met.ivan.devblog.dto.UpdateCategoryRequest;
import met.ivan.devblog.entity.Category;
import met.ivan.devblog.exception.ConflictException;
import met.ivan.devblog.exception.DuplicateResourceException;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.CategoryMapper;
import met.ivan.devblog.repository.CategoryRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.service.CategoryService;
import met.ivan.devblog.util.Slugs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            PostRepository postRepository,
            CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }
        String slug = Slugs.uniqueSlug(request.getName(), categoryRepository::existsBySlug);
        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .build();
        category = categoryRepository.save(category);
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (categoryRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Category with name '" + request.getName() + "' already exists");
        }

        // Update slug only if name changed; ensure uniqueness excluding this category's current slug
        String newSlug = category.getSlug();
        if (!category.getName().equals(request.getName())) {
            String baseSlug = Slugs.toSlug(request.getName());
            // Use existsBySlugAndIdNot to allow keeping same slug if name maps to same slug
            newSlug = Slugs.uniqueSlug(request.getName(),
                    s -> categoryRepository.existsBySlugAndIdNot(s, id));
        }

        category.setName(request.getName());
        category.setSlug(newSlug);
        category.setDescription(request.getDescription());
        category = categoryRepository.save(category);
        return categoryMapper.toResponse(category);
    }

    @Override
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        if (postRepository.existsByCategory(category)) {
            throw new ConflictException("Cannot delete category '" + category.getName() + "' because it is referenced by existing posts");
        }
        categoryRepository.delete(category);
    }
}

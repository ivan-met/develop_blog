package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
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
import met.ivan.devblog.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl")
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private PostRepository postRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = TestDataFactory.category();
    }

    @Test
    @DisplayName("list: returns all categories")
    void list_returnsAll() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(
                new CategoryResponse(1L, "Java", "java", null));

        List<CategoryResponse> result = categoryService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("getBySlug: returns category for known slug")
    void getBySlug_found() {
        when(categoryRepository.findBySlug("java")).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(
                new CategoryResponse(1L, "Java", "java", null));

        CategoryResponse result = categoryService.getBySlug("java");
        assertThat(result.getSlug()).isEqualTo("java");
    }

    @Test
    @DisplayName("getBySlug: throws ResourceNotFoundException for unknown slug")
    void getBySlug_notFound() {
        when(categoryRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getBySlug("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create: creates and returns category with generated slug")
    void create_success() {
        CreateCategoryRequest req = new CreateCategoryRequest("Spring Boot", null);
        when(categoryRepository.existsByName("Spring Boot")).thenReturn(false);
        when(categoryRepository.existsBySlug(anyString())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c = Category.builder().id(2L).name(c.getName()).slug(c.getSlug()).build();
            return c;
        });
        when(categoryMapper.toResponse(any())).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), null);
        });

        CategoryResponse result = categoryService.create(req);

        assertThat(result.getName()).isEqualTo("Spring Boot");
        assertThat(result.getSlug()).isEqualTo("spring-boot");
    }

    @Test
    @DisplayName("create: duplicate name throws DuplicateResourceException")
    void create_duplicateName_throws() {
        when(categoryRepository.existsByName("Java")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CreateCategoryRequest("Java", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Java");
    }

    @Test
    @DisplayName("update: updates name, slug, description")
    void update_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Java Updated", 1L)).thenReturn(false);
        when(categoryRepository.existsBySlugAndIdNot(anyString(), eq(1L))).thenReturn(false);
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toResponse(any())).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription());
        });

        CategoryResponse result = categoryService.update(1L, new UpdateCategoryRequest("Java Updated", "Updated desc"));

        assertThat(result.getName()).isEqualTo("Java Updated");
    }

    @Test
    @DisplayName("update: duplicate name for another category throws")
    void update_duplicateName_throws() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameAndIdNot("Spring", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, new UpdateCategoryRequest("Spring", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("delete: succeeds when no posts reference the category")
    void delete_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postRepository.existsByCategory(category)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("delete: throws ConflictException when posts exist")
    void delete_blocked_whenPostsExist() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postRepository.existsByCategory(category)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("referenced by existing posts");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete: not found throws ResourceNotFoundException")
    void delete_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.AdminCommentResponse;
import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Comment;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.repository.CommentRepository;
import met.ivan.devblog.service.impl.AdminCommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminCommentServiceImpl")
class AdminCommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private AdminCommentServiceImpl adminCommentService;

    private User author;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        Role userRole = TestDataFactory.userRole();
        author = TestDataFactory.userWithRole(userRole);
        Category category = TestDataFactory.category();
        post = TestDataFactory.publishedPost(author, category);
        comment = Comment.builder()
                .id(100L)
                .content("Great article!")
                .author(author)
                .post(post)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("list: returns mapped page when no search is provided")
    void list_noSearch_returnsMappedPage() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> commentPage = new PageImpl<>(List.of(comment), pageable, 1);
        when(commentRepository.findAllWithAuthorAndPost(isNull(), any())).thenReturn(commentPage);

        Page<AdminCommentResponse> result = adminCommentService.list(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        AdminCommentResponse response = result.getContent().get(0);
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getContent()).isEqualTo("Great article!");
        assertThat(response.getPostSlug()).isEqualTo(post.getSlug());
        assertThat(response.getPostTitle()).isEqualTo(post.getTitle());
        assertThat(response.getAuthor().getUsername()).isEqualTo(author.getUsername());
    }

    @Test
    @DisplayName("list: blank search is normalized to null")
    void list_blankSearch_normalizedToNull() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> commentPage = new PageImpl<>(List.of(comment), pageable, 1);
        when(commentRepository.findAllWithAuthorAndPost(isNull(), any())).thenReturn(commentPage);

        Page<AdminCommentResponse> result = adminCommentService.list("   ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(commentRepository).findAllWithAuthorAndPost(isNull(), any());
    }

    @Test
    @DisplayName("list: non-blank search is passed through trimmed")
    void list_withSearch_passedToRepository() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> commentPage = new PageImpl<>(List.of(comment), pageable, 1);
        when(commentRepository.findAllWithAuthorAndPost(eq("java"), any())).thenReturn(commentPage);

        Page<AdminCommentResponse> result = adminCommentService.list("  java  ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(commentRepository).findAllWithAuthorAndPost("java", pageable);
    }

    @Test
    @DisplayName("list: returns empty page when repository returns nothing")
    void list_empty_returnsEmptyPage() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> commentPage = new PageImpl<>(List.of(), pageable, 0);
        when(commentRepository.findAllWithAuthorAndPost(isNull(), any())).thenReturn(commentPage);

        Page<AdminCommentResponse> result = adminCommentService.list(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("list: AdminCommentResponse contains all expected fields")
    void list_responseContainsAllFields() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("createdAt")));
        Page<Comment> commentPage = new PageImpl<>(List.of(comment), pageable, 1);
        when(commentRepository.findAllWithAuthorAndPost(isNull(), any())).thenReturn(commentPage);

        AdminCommentResponse response = adminCommentService.list(null, pageable).getContent().get(0);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getAuthor()).isNotNull();
        assertThat(response.getAuthor().getId()).isEqualTo(author.getId());
        assertThat(response.getAuthor().getUsername()).isEqualTo(author.getUsername());
        assertThat(response.getAuthor().getDisplayName()).isEqualTo(author.getDisplayName());
        assertThat(response.getPostSlug()).isNotBlank();
        assertThat(response.getPostTitle()).isNotBlank();
        assertThat(response.getCreatedAt()).isNotNull();
    }
}

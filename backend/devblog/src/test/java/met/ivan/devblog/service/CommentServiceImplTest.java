package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.CommentResponse;
import met.ivan.devblog.dto.CreateCommentRequest;
import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Comment;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.ForbiddenOperationException;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.CommentMapper;
import met.ivan.devblog.repository.CommentRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl")
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentMapper commentMapper;

    private CommentServiceImpl commentService;

    private Role userRole;
    private Role adminRole;
    private User regularUser;
    private User adminUser;
    private User postAuthor;
    private Category category;
    private Post publishedPost;
    private Post draftPost;

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(commentRepository, postRepository, userRepository, commentMapper);

        userRole = TestDataFactory.userRole();
        adminRole = TestDataFactory.adminRole();
        regularUser = TestDataFactory.userWithRole(userRole);
        adminUser = TestDataFactory.adminUser(adminRole, userRole);
        postAuthor = User.builder()
                .id(99L)
                .username("postauthor")
                .email("postauthor@test.com")
                .passwordHash("hash")
                .active(true)
                .build();
        category = TestDataFactory.category();
        publishedPost = TestDataFactory.publishedPost(postAuthor, category);
        draftPost = TestDataFactory.draftPost(postAuthor, category);
    }

    // --- create ---

    @Test
    @DisplayName("create: rejects comment on draft post (404)")
    void create_draftPost_throwsNotFound() {
        when(postRepository.findBySlugAndStatus("test-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                commentService.create("test-post", "testuser", new CreateCommentRequest("Hello")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("create: saves comment on published post")
    void create_publishedPost_savesComment() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(regularUser));
        Comment savedComment = Comment.builder()
                .id(1L)
                .content("Hello")
                .post(publishedPost)
                .author(regularUser)
                .createdAt(Instant.now())
                .build();
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(commentMapper.toResponse(any(Comment.class), eq(true))).thenReturn(mock(CommentResponse.class));

        commentService.create("published-post", "testuser", new CreateCommentRequest("Hello"));

        verify(commentRepository).save(any(Comment.class));
        verify(commentMapper).toResponse(any(Comment.class), eq(true));
    }

    // --- delete ---

    @Test
    @DisplayName("delete: comment author can delete their own comment")
    void delete_byCommentAuthor_succeeds() {
        Comment comment = buildComment(regularUser, publishedPost);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        UserDetails principal = mockUserDetails("testuser", "ROLE_USER");

        commentService.delete(1L, principal);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("delete: post author can delete any comment on their post")
    void delete_byPostAuthor_succeeds() {
        Comment comment = buildComment(regularUser, publishedPost); // comment by regular user
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        UserDetails postAuthorPrincipal = mockUserDetails("postauthor", "ROLE_USER");

        commentService.delete(1L, postAuthorPrincipal);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("delete: admin can delete any comment")
    void delete_byAdmin_succeeds() {
        Comment comment = buildComment(regularUser, publishedPost);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        UserDetails adminPrincipal = mockUserDetails("admin", "ROLE_ADMIN");

        commentService.delete(1L, adminPrincipal);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("delete: stranger cannot delete comment (403)")
    void delete_byStranger_throwsForbidden() {
        Comment comment = buildComment(regularUser, publishedPost);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        UserDetails stranger = mockUserDetails("stranger", "ROLE_USER");

        assertThatThrownBy(() -> commentService.delete(1L, stranger))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete: not found throws 404")
    void delete_notFound_throwsNotFound() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());
        UserDetails principal = mockUserDetails("testuser", "ROLE_USER");

        assertThatThrownBy(() -> commentService.delete(99L, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- helpers ---

    private Comment buildComment(User author, Post post) {
        return Comment.builder()
                .id(1L)
                .content("Test comment")
                .post(post)
                .author(author)
                .createdAt(Instant.now())
                .build();
    }

    private UserDetails mockUserDetails(String username, String... roles) {
        UserDetails ud = mock(UserDetails.class);
        lenient().when(ud.getUsername()).thenReturn(username);
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        lenient().doReturn(authorities).when(ud).getAuthorities();
        return ud;
    }
}

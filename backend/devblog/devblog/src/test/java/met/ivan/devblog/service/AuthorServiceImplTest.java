package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.AuthorProfileResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.mapper.UserMapper;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.impl.AuthorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorServiceImpl")
class AuthorServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserMapper userMapper;
    @Mock private PostMapper postMapper;

    private AuthorServiceImpl authorService;

    private Role userRole;
    private User user;
    private Category category;
    private Post publishedPost;

    @BeforeEach
    void setUp() {
        authorService = new AuthorServiceImpl(userRepository, postRepository, userMapper, postMapper);

        userRole = TestDataFactory.userRole();
        user = TestDataFactory.userWithRole(userRole);
        category = TestDataFactory.category();
        publishedPost = TestDataFactory.publishedPost(user, category);
    }

    // --- getPublicProfile ---

    @Test
    @DisplayName("getPublicProfile: returns AuthorProfileResponse for known user")
    void getPublicProfile_knownUser_returnsProfile() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postRepository.countByAuthorUsername("testuser")).thenReturn(5L);
        AuthorProfileResponse expected = new AuthorProfileResponse(
                "testuser", "Test User", "Test bio", null, user.getCreatedAt(), 5L);
        when(userMapper.toAuthorProfile(user, 5L)).thenReturn(expected);

        AuthorProfileResponse result = authorService.getPublicProfile("testuser");

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPostCount()).isEqualTo(5L);
        // Must not expose email, roles, id, active
        verify(userMapper).toAuthorProfile(user, 5L);
    }

    @Test
    @DisplayName("getPublicProfile: unknown username throws ResourceNotFoundException")
    void getPublicProfile_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getPublicProfile("nobody"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author not found");
    }

    @Test
    @DisplayName("getPublicProfile: postCount is zero when user has no published posts")
    void getPublicProfile_noPublishedPosts_zeroCount() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postRepository.countByAuthorUsername("testuser")).thenReturn(0L);
        when(userMapper.toAuthorProfile(user, 0L))
                .thenReturn(new AuthorProfileResponse("testuser", null, null, null, user.getCreatedAt(), 0L));

        AuthorProfileResponse result = authorService.getPublicProfile("testuser");

        assertThat(result.getPostCount()).isEqualTo(0L);
    }

    // --- getPublishedPosts ---

    @Test
    @DisplayName("getPublishedPosts: returns published posts for known author")
    void getPublishedPosts_knownAuthor_returnsPosts() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        Page<Post> postPage = new PageImpl<>(List.of(publishedPost));
        when(postRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(postPage);
        when(postMapper.toSummary(any(Post.class))).thenReturn(mock(PostSummaryResponse.class));

        Page<PostSummaryResponse> result = authorService.getPublishedPosts("testuser", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getPublishedPosts: unknown username throws ResourceNotFoundException")
    void getPublishedPosts_unknownAuthor_throwsNotFound() {
        when(userRepository.existsByUsername("nobody")).thenReturn(false);

        assertThatThrownBy(() -> authorService.getPublishedPosts("nobody", PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Author not found");
    }
}

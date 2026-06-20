package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.CreatePostRequest;
import met.ivan.devblog.dto.PostResponse;
import met.ivan.devblog.dto.UpdatePostRequest;
import met.ivan.devblog.dto.UpdatePostStatusRequest;
import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.ForbiddenOperationException;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.repository.CategoryRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.impl.PostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImpl")
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PostMapper postMapper;

    @InjectMocks
    private PostServiceImpl postService;

    private Role userRole;
    private Role adminRole;
    private User regularUser;
    private User adminUser;
    private Category category;
    private Post draftPost;
    private Post publishedPost;

    @BeforeEach
    void setUp() {
        userRole = TestDataFactory.userRole();
        adminRole = TestDataFactory.adminRole();
        regularUser = TestDataFactory.userWithRole(userRole);
        adminUser = TestDataFactory.adminUser(adminRole, userRole);
        category = TestDataFactory.category();
        draftPost = TestDataFactory.draftPost(regularUser, category);
        publishedPost = TestDataFactory.publishedPost(regularUser, category);
    }

    // --- create ---

    @Test
    @DisplayName("create: USER can create a draft post")
    void create_asUser_createsDraft() {
        CreatePostRequest req = new CreatePostRequest("My Title", "Content", null, null, PostStatus.DRAFT);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(regularUser));
        when(postRepository.existsBySlug(anyString())).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p = Post.builder()
                    .id(1L).title(p.getTitle()).slug(p.getSlug())
                    .contentMarkdown(p.getContentMarkdown()).status(p.getStatus())
                    .author(p.getAuthor()).category(p.getCategory())
                    .build();
            return p;
        });
        PostResponse expected = mock(PostResponse.class);
        when(postMapper.toResponse(any())).thenReturn(expected);

        PostResponse result = postService.create("testuser", req);

        assertThat(result).isEqualTo(expected);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("create: ADMIN is denied with ForbiddenOperationException")
    void create_asAdmin_throwsForbidden() {
        CreatePostRequest req = new CreatePostRequest("Title", "Content", null, null, PostStatus.DRAFT);
        when(userRepository.findByUsernameWithRoles("admin")).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> postService.create("admin", req))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Administrators are not permitted");
    }

    @Test
    @DisplayName("create: publish without category throws IllegalArgumentException")
    void create_publishWithoutCategory_throws() {
        CreatePostRequest req = new CreatePostRequest("Title", "Content", null, null, PostStatus.PUBLISHED);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> postService.create("testuser", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category is required");
    }

    @Test
    @DisplayName("create: publish with category sets publishedAt")
    void create_publishWithCategory_setsPublishedAt() {
        CreatePostRequest req = new CreatePostRequest("Title", "Content", null, 1L, PostStatus.PUBLISHED);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(regularUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postRepository.existsBySlug(anyString())).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toResponse(any())).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            // verify publishedAt was set
            assertThat(p.getPublishedAt()).isNotNull();
            return mock(PostResponse.class);
        });

        postService.create("testuser", req);

        verify(postRepository).save(argThat(p -> p.getPublishedAt() != null));
    }

    @Test
    @DisplayName("create: slug uniqueness — appends -2 on collision")
    void create_slugCollision_appendsSuffix() {
        CreatePostRequest req = new CreatePostRequest("My Title", "Content", null, null, PostStatus.DRAFT);
        when(userRepository.findByUsernameWithRoles("testuser")).thenReturn(Optional.of(regularUser));
        // First candidate "my-title" exists, second "my-title-2" does not
        when(postRepository.existsBySlug("my-title")).thenReturn(true);
        when(postRepository.existsBySlug("my-title-2")).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toResponse(any())).thenReturn(mock(PostResponse.class));

        postService.create("testuser", req);

        verify(postRepository).save(argThat(p -> "my-title-2".equals(p.getSlug())));
    }

    // --- update ---

    @Test
    @DisplayName("update: owner can update their own post")
    void update_asOwner_succeeds() {
        UpdatePostRequest req = new UpdatePostRequest("New Title", "New Content", null, null);
        UserDetails principal = mockUserDetails("testuser", "ROLE_USER");
        when(postRepository.findByIdWithAuthorAndCategory(10L)).thenReturn(Optional.of(draftPost));
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toResponse(any())).thenReturn(mock(PostResponse.class));

        postService.update(10L, principal, req);

        assertThat(draftPost.getTitle()).isEqualTo("New Title");
        assertThat(draftPost.getSlug()).isEqualTo("test-post"); // slug unchanged
    }

    @Test
    @DisplayName("update: admin can update another user's post")
    void update_asAdmin_succeeds() {
        UpdatePostRequest req = new UpdatePostRequest("Admin Edit", "Content", null, null);
        UserDetails adminPrincipal = mockUserDetails("admin", "ROLE_ADMIN");
        when(postRepository.findByIdWithAuthorAndCategory(10L)).thenReturn(Optional.of(draftPost));
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toResponse(any())).thenReturn(mock(PostResponse.class));

        postService.update(10L, adminPrincipal, req);

        assertThat(draftPost.getTitle()).isEqualTo("Admin Edit");
    }

    @Test
    @DisplayName("update: non-owner non-admin gets 403")
    void update_asNonOwner_throwsForbidden() {
        UpdatePostRequest req = new UpdatePostRequest("Hack", "Content", null, null);
        UserDetails stranger = mockUserDetails("stranger", "ROLE_USER");
        when(postRepository.findByIdWithAuthorAndCategory(10L)).thenReturn(Optional.of(draftPost));

        assertThatThrownBy(() -> postService.update(10L, stranger, req))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    // --- changeStatus ---

    @Test
    @DisplayName("changeStatus: first publish sets publishedAt")
    void changeStatus_firstPublish_setsPublishedAt() {
        assertThat(draftPost.getPublishedAt()).isNull();
        draftPost.setCategory(category); // has category

        UpdatePostStatusRequest req = new UpdatePostStatusRequest(PostStatus.PUBLISHED);
        UserDetails principal = mockUserDetails("testuser", "ROLE_USER");
        when(postRepository.findByIdWithAuthorAndCategory(10L)).thenReturn(Optional.of(draftPost));
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(postMapper.toResponse(any())).thenReturn(mock(PostResponse.class));

        postService.changeStatus(10L, principal, req);

        assertThat(draftPost.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("changeStatus: publish without category throws 400")
    void changeStatus_publishWithoutCategory_throws() {
        draftPost.setCategory(null);
        UpdatePostStatusRequest req = new UpdatePostStatusRequest(PostStatus.PUBLISHED);
        UserDetails principal = mockUserDetails("testuser", "ROLE_USER");
        when(postRepository.findByIdWithAuthorAndCategory(10L)).thenReturn(Optional.of(draftPost));

        assertThatThrownBy(() -> postService.changeStatus(10L, principal, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("category is required");
    }

    @Test
    @DisplayName("changeStatus: non-owner gets 403")
    void changeStatus_nonOwner_throwsForbidden() {
        UpdatePostStatusRequest req = new UpdatePostStatusRequest(PostStatus.PUBLISHED);
        UserDetails stranger = mockUserDetails("stranger", "ROLE_USER");
        when(postRepository.findByIdWithAuthorAndCategory(10L)).thenReturn(Optional.of(draftPost));

        assertThatThrownBy(() -> postService.changeStatus(10L, stranger, req))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    // --- delete ---

    @Test
    @DisplayName("delete: owner can delete their own post")
    void delete_asOwner_succeeds() {
        UserDetails principal = mockUserDetails("testuser", "ROLE_USER");
        when(postRepository.findByIdWithAuthorAndCategory(10L)).thenReturn(Optional.of(draftPost));

        postService.delete(10L, principal);

        verify(postRepository).delete(draftPost);
    }

    @Test
    @DisplayName("delete: admin can delete any post")
    void delete_asAdmin_succeeds() {
        UserDetails adminPrincipal = mockUserDetails("admin", "ROLE_ADMIN");
        when(postRepository.findByIdWithAuthorAndCategory(10L)).thenReturn(Optional.of(draftPost));

        postService.delete(10L, adminPrincipal);

        verify(postRepository).delete(draftPost);
    }

    @Test
    @DisplayName("delete: non-owner non-admin gets 403")
    void delete_asStranger_throwsForbidden() {
        UserDetails stranger = mockUserDetails("stranger", "ROLE_USER");
        when(postRepository.findByIdWithAuthorAndCategory(10L)).thenReturn(Optional.of(draftPost));

        assertThatThrownBy(() -> postService.delete(10L, stranger))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("delete: not found throws ResourceNotFoundException")
    void delete_notFound_throws() {
        UserDetails principal = mockUserDetails("testuser", "ROLE_USER");
        when(postRepository.findByIdWithAuthorAndCategory(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.delete(99L, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getPublishedBySlug ---

    @Test
    @DisplayName("getPublishedBySlug: returns published post")
    void getPublishedBySlug_found() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(postMapper.toResponse(publishedPost)).thenReturn(mock(PostResponse.class));

        PostResponse result = postService.getPublishedBySlug("published-post");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getPublishedBySlug: draft returns 404")
    void getPublishedBySlug_draft_throwsNotFound() {
        when(postRepository.findBySlugAndStatus("test-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPublishedBySlug("test-post"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- helpers ---

    private UserDetails mockUserDetails(String username, String... roles) {
        UserDetails ud = mock(UserDetails.class);
        lenient().when(ud.getUsername()).thenReturn(username);
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        lenient().doReturn(authorities).when(ud).getAuthorities();
        return ud;
    }
}

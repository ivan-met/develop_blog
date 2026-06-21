package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.BookmarkResponse;
import met.ivan.devblog.dto.LikeResponse;
import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostBookmark;
import met.ivan.devblog.entity.PostLike;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.repository.PostBookmarkRepository;
import met.ivan.devblog.repository.PostLikeRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.impl.EngagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EngagementServiceImpl")
class EngagementServiceImplTest {

    @Mock private PostLikeRepository postLikeRepository;
    @Mock private PostBookmarkRepository postBookmarkRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostMapper postMapper;

    private EngagementServiceImpl engagementService;

    private Role userRole;
    private User user;
    private Category category;
    private Post publishedPost;

    @BeforeEach
    void setUp() {
        engagementService = new EngagementServiceImpl(
                postLikeRepository, postBookmarkRepository, postRepository, userRepository, postMapper);

        userRole = TestDataFactory.userRole();
        user = TestDataFactory.userWithRole(userRole);
        category = TestDataFactory.category();
        publishedPost = TestDataFactory.publishedPost(user, category);
    }

    // --- like ---

    @Test
    @DisplayName("like: creates a PostLike when not already liked")
    void like_notYetLiked_createsLike() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postLikeRepository.existsByUserIdAndPostId(user.getId(), publishedPost.getId())).thenReturn(false);
        when(postLikeRepository.save(any(PostLike.class))).thenAnswer(inv -> inv.getArgument(0));
        when(postLikeRepository.countByPostId(publishedPost.getId())).thenReturn(1L);

        LikeResponse resp = engagementService.like("published-post", "testuser");

        assertThat(resp.getLikeCount()).isEqualTo(1);
        assertThat(resp.isLiked()).isTrue();
        verify(postLikeRepository).save(any(PostLike.class));
    }

    @Test
    @DisplayName("like: idempotent — does not create duplicate when already liked")
    void like_alreadyLiked_idempotent() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postLikeRepository.existsByUserIdAndPostId(user.getId(), publishedPost.getId())).thenReturn(true);
        when(postLikeRepository.countByPostId(publishedPost.getId())).thenReturn(1L);

        LikeResponse resp = engagementService.like("published-post", "testuser");

        assertThat(resp.isLiked()).isTrue();
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    @DisplayName("like: unknown slug throws ResourceNotFoundException")
    void like_unknownSlug_throwsNotFound() {
        when(postRepository.findBySlugAndStatus("bad-slug", PostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> engagementService.like("bad-slug", "testuser"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- unlike ---

    @Test
    @DisplayName("unlike: removes like when it exists")
    void unlike_whenLiked_removesLike() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postLikeRepository.existsByUserIdAndPostId(user.getId(), publishedPost.getId())).thenReturn(true);
        when(postLikeRepository.countByPostId(publishedPost.getId())).thenReturn(0L);

        LikeResponse resp = engagementService.unlike("published-post", "testuser");

        assertThat(resp.isLiked()).isFalse();
        assertThat(resp.getLikeCount()).isEqualTo(0);
        verify(postLikeRepository).deleteByUserIdAndPostId(user.getId(), publishedPost.getId());
    }

    @Test
    @DisplayName("unlike: idempotent — no error when not already liked")
    void unlike_notLiked_idempotent() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postLikeRepository.existsByUserIdAndPostId(user.getId(), publishedPost.getId())).thenReturn(false);
        when(postLikeRepository.countByPostId(publishedPost.getId())).thenReturn(0L);

        LikeResponse resp = engagementService.unlike("published-post", "testuser");

        assertThat(resp.isLiked()).isFalse();
        verify(postLikeRepository, never()).deleteByUserIdAndPostId(any(), any());
    }

    // --- bookmark ---

    @Test
    @DisplayName("bookmark: creates a PostBookmark when not already bookmarked")
    void bookmark_notYetBookmarked_creates() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), publishedPost.getId())).thenReturn(false);
        when(postBookmarkRepository.save(any(PostBookmark.class))).thenAnswer(inv -> inv.getArgument(0));

        BookmarkResponse resp = engagementService.bookmark("published-post", "testuser");

        assertThat(resp.isBookmarked()).isTrue();
        verify(postBookmarkRepository).save(any(PostBookmark.class));
    }

    @Test
    @DisplayName("bookmark: idempotent — does not create duplicate when already bookmarked")
    void bookmark_alreadyBookmarked_idempotent() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), publishedPost.getId())).thenReturn(true);

        BookmarkResponse resp = engagementService.bookmark("published-post", "testuser");

        assertThat(resp.isBookmarked()).isTrue();
        verify(postBookmarkRepository, never()).save(any());
    }

    // --- removeBookmark ---

    @Test
    @DisplayName("removeBookmark: removes bookmark when it exists")
    void removeBookmark_whenBookmarked_removes() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), publishedPost.getId())).thenReturn(true);

        BookmarkResponse resp = engagementService.removeBookmark("published-post", "testuser");

        assertThat(resp.isBookmarked()).isFalse();
        verify(postBookmarkRepository).deleteByUserIdAndPostId(user.getId(), publishedPost.getId());
    }

    @Test
    @DisplayName("removeBookmark: idempotent — no error when not bookmarked")
    void removeBookmark_notBookmarked_idempotent() {
        when(postRepository.findBySlugAndStatus("published-post", PostStatus.PUBLISHED))
                .thenReturn(Optional.of(publishedPost));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(postBookmarkRepository.existsByUserIdAndPostId(user.getId(), publishedPost.getId())).thenReturn(false);

        BookmarkResponse resp = engagementService.removeBookmark("published-post", "testuser");

        assertThat(resp.isBookmarked()).isFalse();
        verify(postBookmarkRepository, never()).deleteByUserIdAndPostId(any(), any());
    }
}

package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.PlatformStatsResponse;
import met.ivan.devblog.entity.Category;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.repository.CategoryRepository;
import met.ivan.devblog.repository.CommentRepository;
import met.ivan.devblog.repository.PostBookmarkRepository;
import met.ivan.devblog.repository.PostLikeRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.impl.AdminStatsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatsServiceImpl")
class AdminStatsServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private PostBookmarkRepository postBookmarkRepository;
    @Mock private PostMapper postMapper;

    @InjectMocks
    private AdminStatsServiceImpl adminStatsService;

    private User author;
    private Category category;
    private Post publishedPost;

    @BeforeEach
    void setUp() {
        Role userRole = TestDataFactory.userRole();
        author = TestDataFactory.userWithRole(userRole);
        category = TestDataFactory.category();
        publishedPost = TestDataFactory.publishedPost(author, category);
    }

    @Test
    @DisplayName("getStats: totals are aggregated from all repositories")
    void getStats_aggregatesTotals() {
        stubTotals(10L, 8L, 20L, 15L, 5L, 50L, 4L, 100L, 30L);
        stubTopByViews(List.of());
        stubTopByLikes(List.of());
        stubRecentUsers(List.of());

        PlatformStatsResponse stats = adminStatsService.getStats();

        PlatformStatsResponse.StatsTotals totals = stats.totals();
        assertThat(totals.users()).isEqualTo(10L);
        assertThat(totals.activeUsers()).isEqualTo(8L);
        assertThat(totals.posts()).isEqualTo(20L);
        assertThat(totals.publishedPosts()).isEqualTo(15L);
        assertThat(totals.draftPosts()).isEqualTo(5L);
        assertThat(totals.comments()).isEqualTo(50L);
        assertThat(totals.categories()).isEqualTo(4L);
        assertThat(totals.likes()).isEqualTo(100L);
        assertThat(totals.bookmarks()).isEqualTo(30L);
    }

    @Test
    @DisplayName("getStats: topPostsByViews is populated from repository")
    void getStats_topByViews_populated() {
        stubTotals(1L, 1L, 1L, 1L, 0L, 0L, 1L, 0L, 0L);
        stubTopByViews(List.of(publishedPost));
        when(postMapper.toSummary(publishedPost)).thenReturn(
                new met.ivan.devblog.dto.PostSummaryResponse(
                        publishedPost.getId(), publishedPost.getSlug(), publishedPost.getTitle(),
                        publishedPost.getExcerpt(), publishedPost.getStatus(), null, null,
                        publishedPost.getPublishedAt(), publishedPost.getCreatedAt(),
                        publishedPost.getTags(), publishedPost.getViewCount(), null));
        stubTopByLikes(List.of());
        stubRecentUsers(List.of());

        PlatformStatsResponse stats = adminStatsService.getStats();

        assertThat(stats.topPostsByViews()).hasSize(1);
        assertThat(stats.topPostsByViews().get(0).getSlug()).isEqualTo(publishedPost.getSlug());
    }

    @Test
    @DisplayName("getStats: topPostsByLikes maps post and likeCount correctly")
    void getStats_topByLikes_mapsCorrectly() {
        stubTotals(1L, 1L, 1L, 1L, 0L, 0L, 1L, 5L, 0L);
        stubTopByViews(List.of());
        stubTopByLikes(List.<Object[]>of(new Object[]{publishedPost, 5L}));
        stubRecentUsers(List.of());

        PlatformStatsResponse stats = adminStatsService.getStats();

        assertThat(stats.topPostsByLikes()).hasSize(1);
        assertThat(stats.topPostsByLikes().get(0).slug()).isEqualTo(publishedPost.getSlug());
        assertThat(stats.topPostsByLikes().get(0).likeCount()).isEqualTo(5L);
        assertThat(stats.topPostsByLikes().get(0).author().getUsername()).isEqualTo(author.getUsername());
    }

    @Test
    @DisplayName("getStats: recentUsers maps username, displayName, createdAt")
    void getStats_recentUsers_mapsCorrectly() {
        stubTotals(1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        stubTopByViews(List.of());
        stubTopByLikes(List.of());
        stubRecentUsers(List.of(author));

        PlatformStatsResponse stats = adminStatsService.getStats();

        assertThat(stats.recentUsers()).hasSize(1);
        assertThat(stats.recentUsers().get(0).username()).isEqualTo(author.getUsername());
        assertThat(stats.recentUsers().get(0).displayName()).isEqualTo(author.getDisplayName());
        assertThat(stats.recentUsers().get(0).createdAt()).isEqualTo(author.getCreatedAt());
    }

    @Test
    @DisplayName("getStats: returns empty lists when no data exists")
    void getStats_emptyPlatform_returnsZerosAndEmptyLists() {
        stubTotals(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        stubTopByViews(List.of());
        stubTopByLikes(List.of());
        stubRecentUsers(List.of());

        PlatformStatsResponse stats = adminStatsService.getStats();

        assertThat(stats.totals().users()).isEqualTo(0L);
        assertThat(stats.topPostsByViews()).isEmpty();
        assertThat(stats.topPostsByLikes()).isEmpty();
        assertThat(stats.recentUsers()).isEmpty();
    }

    // --- Stub helpers ---

    private void stubTotals(long users, long activeUsers, long posts, long published, long draft,
                             long comments, long categories, long likes, long bookmarks) {
        when(userRepository.count()).thenReturn(users);
        when(userRepository.countByActiveTrue()).thenReturn(activeUsers);
        when(postRepository.count()).thenReturn(posts);
        when(postRepository.countByStatus(PostStatus.PUBLISHED)).thenReturn(published);
        when(postRepository.countByStatus(PostStatus.DRAFT)).thenReturn(draft);
        when(commentRepository.count()).thenReturn(comments);
        when(categoryRepository.count()).thenReturn(categories);
        when(postLikeRepository.count()).thenReturn(likes);
        when(postBookmarkRepository.count()).thenReturn(bookmarks);
    }

    private void stubTopByViews(List<Post> posts) {
        when(postRepository.findTopByStatusOrderByViewCountDesc(eq(PostStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(posts);
    }

    private void stubTopByLikes(List<Object[]> rows) {
        when(postLikeRepository.findTopPostsByLikeCount(any(Pageable.class))).thenReturn(rows);
    }

    private void stubRecentUsers(List<User> users) {
        when(userRepository.findTop5ByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(users);
    }
}

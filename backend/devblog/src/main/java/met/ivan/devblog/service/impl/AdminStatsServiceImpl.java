package met.ivan.devblog.service.impl;

import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.PlatformStatsResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.dto.RecentUserResponse;
import met.ivan.devblog.dto.TopPostResponse;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.repository.CategoryRepository;
import met.ivan.devblog.repository.CommentRepository;
import met.ivan.devblog.repository.PostBookmarkRepository;
import met.ivan.devblog.repository.PostLikeRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.AdminStatsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminStatsServiceImpl implements AdminStatsService {

    private static final int TOP_N = 5;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CategoryRepository categoryRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final PostMapper postMapper;

    public AdminStatsServiceImpl(
            UserRepository userRepository,
            PostRepository postRepository,
            CommentRepository commentRepository,
            CategoryRepository categoryRepository,
            PostLikeRepository postLikeRepository,
            PostBookmarkRepository postBookmarkRepository,
            PostMapper postMapper) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.categoryRepository = categoryRepository;
        this.postLikeRepository = postLikeRepository;
        this.postBookmarkRepository = postBookmarkRepository;
        this.postMapper = postMapper;
    }

    @Override
    public PlatformStatsResponse getStats() {
        PlatformStatsResponse.StatsTotals totals = buildTotals();
        List<PostSummaryResponse> topByViews = buildTopByViews();
        List<TopPostResponse> topByLikes = buildTopByLikes();
        List<RecentUserResponse> recentUsers = buildRecentUsers();
        return new PlatformStatsResponse(totals, topByViews, topByLikes, recentUsers);
    }

    private PlatformStatsResponse.StatsTotals buildTotals() {
        long users = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long posts = postRepository.count();
        long publishedPosts = postRepository.countByStatus(PostStatus.PUBLISHED);
        long draftPosts = postRepository.countByStatus(PostStatus.DRAFT);
        long comments = commentRepository.count();
        long categories = categoryRepository.count();
        long likes = postLikeRepository.count();
        long bookmarks = postBookmarkRepository.count();
        return new PlatformStatsResponse.StatsTotals(
                users, activeUsers, posts, publishedPosts, draftPosts,
                comments, categories, likes, bookmarks
        );
    }

    private List<PostSummaryResponse> buildTopByViews() {
        List<Post> posts = postRepository.findTopByStatusOrderByViewCountDesc(
                PostStatus.PUBLISHED, PageRequest.of(0, TOP_N));
        return posts.stream()
                .map(postMapper::toSummary)
                .toList();
    }

    private List<TopPostResponse> buildTopByLikes() {
        List<Object[]> rows = postLikeRepository.findTopPostsByLikeCount(PageRequest.of(0, TOP_N));
        return rows.stream()
                .map(row -> {
                    Post post = (Post) row[0];
                    long likeCount = (Long) row[1];
                    AuthorSummary author = new AuthorSummary(
                            post.getAuthor().getId(),
                            post.getAuthor().getUsername(),
                            post.getAuthor().getDisplayName()
                    );
                    return new TopPostResponse(post.getSlug(), post.getTitle(), author, likeCount);
                })
                .toList();
    }

    private List<RecentUserResponse> buildRecentUsers() {
        List<User> users = userRepository.findTop5ByOrderByCreatedAtDesc(PageRequest.of(0, TOP_N));
        return users.stream()
                .map(u -> new RecentUserResponse(u.getUsername(), u.getDisplayName(), u.getCreatedAt()))
                .toList();
    }
}

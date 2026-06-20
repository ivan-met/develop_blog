package met.ivan.devblog.service.impl;

import met.ivan.devblog.dto.BookmarkResponse;
import met.ivan.devblog.dto.LikeResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.Post;
import met.ivan.devblog.entity.PostBookmark;
import met.ivan.devblog.entity.PostLike;
import met.ivan.devblog.entity.PostStatus;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.PostMapper;
import met.ivan.devblog.repository.PostBookmarkRepository;
import met.ivan.devblog.repository.PostLikeRepository;
import met.ivan.devblog.repository.PostRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.EngagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EngagementServiceImpl implements EngagementService {

    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    public EngagementServiceImpl(
            PostLikeRepository postLikeRepository,
            PostBookmarkRepository postBookmarkRepository,
            PostRepository postRepository,
            UserRepository userRepository,
            PostMapper postMapper) {
        this.postLikeRepository = postLikeRepository;
        this.postBookmarkRepository = postBookmarkRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postMapper = postMapper;
    }

    @Override
    public LikeResponse like(String slug, String username) {
        Post post = loadPublishedPost(slug);
        User user = loadUser(username);

        if (!postLikeRepository.existsByUserIdAndPostId(user.getId(), post.getId())) {
            PostLike like = PostLike.builder()
                    .user(user)
                    .post(post)
                    .build();
            postLikeRepository.save(like);
        }

        long count = postLikeRepository.countByPostId(post.getId());
        return new LikeResponse(count, true);
    }

    @Override
    public LikeResponse unlike(String slug, String username) {
        Post post = loadPublishedPost(slug);
        User user = loadUser(username);

        if (postLikeRepository.existsByUserIdAndPostId(user.getId(), post.getId())) {
            postLikeRepository.deleteByUserIdAndPostId(user.getId(), post.getId());
        }

        long count = postLikeRepository.countByPostId(post.getId());
        return new LikeResponse(count, false);
    }

    @Override
    public BookmarkResponse bookmark(String slug, String username) {
        Post post = loadPublishedPost(slug);
        User user = loadUser(username);

        if (!postBookmarkRepository.existsByUserIdAndPostId(user.getId(), post.getId())) {
            PostBookmark bookmark = PostBookmark.builder()
                    .user(user)
                    .post(post)
                    .build();
            postBookmarkRepository.save(bookmark);
        }

        return new BookmarkResponse(true);
    }

    @Override
    public BookmarkResponse removeBookmark(String slug, String username) {
        Post post = loadPublishedPost(slug);
        User user = loadUser(username);

        if (postBookmarkRepository.existsByUserIdAndPostId(user.getId(), post.getId())) {
            postBookmarkRepository.deleteByUserIdAndPostId(user.getId(), post.getId());
        }

        return new BookmarkResponse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listBookmarks(String username, Pageable pageable) {
        User user = loadUser(username);
        return postBookmarkRepository.findBookmarkedPostsByUserId(user.getId(), pageable)
                .map(postMapper::toSummary);
    }

    // --- helpers ---

    private Post loadPublishedPost(String slug) {
        return postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published post not found with slug: " + slug));
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}

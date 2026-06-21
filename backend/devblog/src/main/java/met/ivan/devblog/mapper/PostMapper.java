package met.ivan.devblog.mapper;

import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.CategoryResponse;
import met.ivan.devblog.dto.PostResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.Post;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class PostMapper {

    private final CategoryMapper categoryMapper;

    public PostMapper(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /**
     * Map to full PostResponse without engagement data (anonymous/unenriched context).
     */
    public PostResponse toResponse(Post post) {
        return toResponse(post, null, null, null);
    }

    /**
     * Map to full PostResponse with engagement data (authenticated context).
     *
     * @param likeCount  total likes for this post, or null if unknown
     * @param liked      whether the current user liked this post, or null if anonymous
     * @param bookmarked whether the current user bookmarked this post, or null if anonymous
     */
    public PostResponse toResponse(Post post, Long likeCount, Boolean liked, Boolean bookmarked) {
        return new PostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getStatus(),
                categoryMapper.toResponse(post.getCategory()),
                toAuthorSummary(post),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getContentMarkdown(),
                post.getUpdatedAt(),
                copyTags(post),
                post.getViewCount(),
                likeCount,
                liked,
                bookmarked
        );
    }

    /**
     * Map to PostSummaryResponse without engagement data.
     */
    public PostSummaryResponse toSummary(Post post) {
        return toSummary(post, null);
    }

    /**
     * Map to PostSummaryResponse with a pre-computed like count.
     *
     * @param likeCount total likes for this post, or null if unknown
     */
    public PostSummaryResponse toSummary(Post post, Long likeCount) {
        return new PostSummaryResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getStatus(),
                categoryMapper.toResponse(post.getCategory()),
                toAuthorSummary(post),
                post.getPublishedAt(),
                post.getCreatedAt(),
                copyTags(post),
                post.getViewCount(),
                likeCount
        );
    }

    private Set<String> copyTags(Post post) {
        return post.getTags() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(post.getTags());
    }

    private AuthorSummary toAuthorSummary(Post post) {
        return new AuthorSummary(
                post.getAuthor().getId(),
                post.getAuthor().getUsername(),
                post.getAuthor().getDisplayName()
        );
    }
}

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

    public PostResponse toResponse(Post post) {
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
                post.getViewCount()
        );
    }

    public PostSummaryResponse toSummary(Post post) {
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
                post.getViewCount()
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

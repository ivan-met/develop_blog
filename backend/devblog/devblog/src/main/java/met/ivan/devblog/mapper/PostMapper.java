package met.ivan.devblog.mapper;

import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.CategoryResponse;
import met.ivan.devblog.dto.PostResponse;
import met.ivan.devblog.dto.PostSummaryResponse;
import met.ivan.devblog.entity.Post;
import org.springframework.stereotype.Component;

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
                post.getUpdatedAt()
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
                post.getCreatedAt()
        );
    }

    private AuthorSummary toAuthorSummary(Post post) {
        return new AuthorSummary(
                post.getAuthor().getId(),
                post.getAuthor().getUsername(),
                post.getAuthor().getDisplayName()
        );
    }
}

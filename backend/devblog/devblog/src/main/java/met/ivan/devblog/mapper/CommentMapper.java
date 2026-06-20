package met.ivan.devblog.mapper;

import met.ivan.devblog.dto.AuthorSummary;
import met.ivan.devblog.dto.CommentResponse;
import met.ivan.devblog.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    /**
     * Map a Comment to CommentResponse.
     *
     * @param comment   the comment entity (author must be loaded)
     * @param canDelete whether the requesting principal can delete this comment
     */
    public CommentResponse toResponse(Comment comment, boolean canDelete) {
        AuthorSummary authorSummary = new AuthorSummary(
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getAuthor().getDisplayName()
        );
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                authorSummary,
                comment.getCreatedAt(),
                canDelete
        );
    }
}

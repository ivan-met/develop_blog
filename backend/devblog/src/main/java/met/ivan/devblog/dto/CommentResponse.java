package met.ivan.devblog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class CommentResponse {
    private final Long id;
    private final String content;
    private final AuthorSummary author;
    private final Instant createdAt;
    /** Whether the requesting principal is allowed to delete this comment. */
    private final boolean canDelete;
}

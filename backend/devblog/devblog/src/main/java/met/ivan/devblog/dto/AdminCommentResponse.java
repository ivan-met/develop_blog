package met.ivan.devblog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * Richer comment DTO for admin use: includes the post context (slug + title) so
 * an admin can identify which post each comment belongs to.
 */
@Getter
@AllArgsConstructor
public class AdminCommentResponse {
    private final Long id;
    private final String content;
    private final AuthorSummary author;
    private final String postSlug;
    private final String postTitle;
    private final Instant createdAt;
}

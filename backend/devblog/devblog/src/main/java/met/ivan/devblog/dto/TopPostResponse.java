package met.ivan.devblog.dto;

/**
 * Lightweight post summary used in stats top-by-likes list.
 */
public record TopPostResponse(
        String slug,
        String title,
        AuthorSummary author,
        long likeCount
) {}

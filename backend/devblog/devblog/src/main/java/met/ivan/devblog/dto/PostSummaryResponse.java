package met.ivan.devblog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import met.ivan.devblog.entity.PostStatus;

import java.time.Instant;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostSummaryResponse {
    private final Long id;
    private final String slug;
    private final String title;
    private final String excerpt;
    private final PostStatus status;
    private final CategoryResponse category;
    private final AuthorSummary author;
    private final Instant publishedAt;
    private final Instant createdAt;
}

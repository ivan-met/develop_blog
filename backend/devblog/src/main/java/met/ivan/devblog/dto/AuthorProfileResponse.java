package met.ivan.devblog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * Public author profile — deliberately omits email, roles, id, and active status.
 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorProfileResponse {
    private final String username;
    private final String displayName;
    private final String bio;
    private final String avatarUrl;
    private final Instant createdAt;
    private final long postCount;
}

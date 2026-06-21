package met.ivan.devblog.dto;

import java.time.Instant;

/**
 * Lightweight user summary used in stats recent-users list.
 */
public record RecentUserResponse(
        String username,
        String displayName,
        Instant createdAt
) {}

package met.ivan.devblog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserResponse {
    private final Long id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String bio;
    private final String avatarUrl;
    private final List<String> roles;
    private final boolean active;
    private final Instant createdAt;
}

package met.ivan.devblog.mapper;

import met.ivan.devblog.dto.AuthorProfileResponse;
import met.ivan.devblog.dto.UserResponse;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .sorted()
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                roles,
                user.isActive(),
                user.getCreatedAt()
        );
    }

    /**
     * Map to public author profile — deliberately omits email, roles, id, and active status.
     */
    public AuthorProfileResponse toAuthorProfile(User user, long postCount) {
        return new AuthorProfileResponse(
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                postCount
        );
    }
}

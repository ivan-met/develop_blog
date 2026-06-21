package met.ivan.devblog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    @Size(max = 255, message = "Avatar URL must be at most 255 characters")
    private String avatarUrl;

    @Email(message = "Email must be valid")
    private String email;
}

package met.ivan.devblog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenRefreshResponse {
    private final String accessToken;
    private final String refreshToken;
}

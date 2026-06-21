package met.ivan.devblog.service;

import met.ivan.devblog.dto.*;

public interface AuthenticationService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    TokenRefreshResponse refresh(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
}

package met.ivan.devblog.controller;

import jakarta.validation.Valid;
import met.ivan.devblog.dto.*;
import met.ivan.devblog.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    public TokenRefreshResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authenticationService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authenticationService.logout(request);
    }
}

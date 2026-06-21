package met.ivan.devblog.service;

import met.ivan.devblog.dto.ChangePasswordRequest;
import met.ivan.devblog.dto.UpdateProfileRequest;
import met.ivan.devblog.dto.UserResponse;

public interface UserService {
    UserResponse getProfile(String username);
    UserResponse updateProfile(String username, UpdateProfileRequest request);
    void changePassword(String username, ChangePasswordRequest request);
}

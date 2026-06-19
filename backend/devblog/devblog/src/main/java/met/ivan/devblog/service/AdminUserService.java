package met.ivan.devblog.service;

import met.ivan.devblog.dto.UpdateRolesRequest;
import met.ivan.devblog.dto.UpdateStatusRequest;
import met.ivan.devblog.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<UserResponse> listUsers(String search, Pageable pageable);
    UserResponse getUser(Long id);
    UserResponse updateRoles(Long id, UpdateRolesRequest request);
    UserResponse updateStatus(Long id, UpdateStatusRequest request);
}

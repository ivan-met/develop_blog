package met.ivan.devblog.controller;

import jakarta.validation.Valid;
import met.ivan.devblog.dto.UpdateRolesRequest;
import met.ivan.devblog.dto.UpdateStatusRequest;
import met.ivan.devblog.dto.UserResponse;
import met.ivan.devblog.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Page<UserResponse> listUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return adminUserService.listUsers(search, pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return adminUserService.getUser(id);
    }

    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRolesRequest request) {
        return adminUserService.updateRoles(id, request);
    }

    @PutMapping("/{id}/status")
    public UserResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return adminUserService.updateStatus(id, request);
    }
}

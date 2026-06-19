package met.ivan.devblog.service;

import met.ivan.devblog.TestDataFactory;
import met.ivan.devblog.dto.UpdateRolesRequest;
import met.ivan.devblog.dto.UpdateStatusRequest;
import met.ivan.devblog.dto.UserResponse;
import met.ivan.devblog.entity.Role;
import met.ivan.devblog.entity.RoleName;
import met.ivan.devblog.entity.User;
import met.ivan.devblog.exception.ResourceNotFoundException;
import met.ivan.devblog.mapper.UserMapper;
import met.ivan.devblog.repository.RoleRepository;
import met.ivan.devblog.repository.UserRepository;
import met.ivan.devblog.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserServiceImpl")
class AdminUserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private Role userRole;
    private Role adminRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRole = TestDataFactory.userRole();
        adminRole = TestDataFactory.adminRole();
        testUser = TestDataFactory.userWithRole(userRole);
    }

    @Test
    @DisplayName("listUsers: returns paginated results")
    void listUsers_returnsPaginatedResults() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
        UserResponse userResponse = new UserResponse(1L, "testuser", "test@example.com",
                null, null, null, List.of("USER"), true, Instant.now());

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toResponse(testUser)).thenReturn(userResponse);

        Page<UserResponse> result = adminUserService.listUsers(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUser: returns user for valid id")
    void getUser_validId() {
        UserResponse expected = new UserResponse(1L, "testuser", "test@example.com",
                null, null, null, List.of("USER"), true, Instant.now());
        when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(expected);

        UserResponse result = adminUserService.getUser(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getUser: throws ResourceNotFoundException for unknown id")
    void getUser_unknownId() {
        when(userRepository.findByIdWithRoles(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateRoles: assigns ADMIN role to user")
    void updateRoles_assignsAdminRole() {
        when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any())).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(
                new UserResponse(1L, "testuser", "test@example.com", null, null, null,
                        List.of("ADMIN"), true, Instant.now()));

        UpdateRolesRequest request = new UpdateRolesRequest(List.of("ADMIN"));
        UserResponse result = adminUserService.updateRoles(1L, request);

        assertThat(testUser.getRoles()).contains(adminRole);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateRoles: user can hold both ADMIN and USER")
    void updateRoles_holdsBothRoles() {
        when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any())).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(
                new UserResponse(1L, "testuser", "test@example.com", null, null, null,
                        List.of("ADMIN", "USER"), true, Instant.now()));

        UpdateRolesRequest request = new UpdateRolesRequest(List.of("ADMIN", "USER"));
        adminUserService.updateRoles(1L, request);

        assertThat(testUser.getRoles()).containsExactlyInAnyOrder(adminRole, userRole);
    }

    @Test
    @DisplayName("updateStatus: deactivates user")
    void updateStatus_deactivatesUser() {
        when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(
                new UserResponse(1L, "testuser", "test@example.com", null, null, null,
                        List.of("USER"), false, Instant.now()));

        UpdateStatusRequest request = new UpdateStatusRequest(false);
        adminUserService.updateStatus(1L, request);

        assertThat(testUser.isActive()).isFalse();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateStatus: reactivates user")
    void updateStatus_reactivatesUser() {
        testUser.setActive(false);
        when(userRepository.findByIdWithRoles(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(
                new UserResponse(1L, "testuser", "test@example.com", null, null, null,
                        List.of("USER"), true, Instant.now()));

        UpdateStatusRequest request = new UpdateStatusRequest(true);
        adminUserService.updateStatus(1L, request);

        assertThat(testUser.isActive()).isTrue();
    }
}

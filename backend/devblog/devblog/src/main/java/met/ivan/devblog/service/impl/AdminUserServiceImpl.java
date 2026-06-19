package met.ivan.devblog.service.impl;

import jakarta.persistence.criteria.Predicate;
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
import met.ivan.devblog.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public AdminUserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String search, Pageable pageable) {
        Specification<User> spec = buildSearchSpec(search);
        return userRepository.findAll(spec, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateRoles(Long id, UpdateRolesRequest request) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Set<Role> newRoles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            RoleName rn;
            try {
                rn = RoleName.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Role not found: " + roleName);
            }
            Role role = roleRepository.findByName(rn)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            newRoles.add(role);
        }

        user.setRoles(newRoles);
        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateStatus(Long id, UpdateStatusRequest request) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setActive(request.getActive());
        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    private Specification<User> buildSearchSpec(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("username")), pattern));
            predicates.add(cb.like(cb.lower(root.get("email")), pattern));
            predicates.add(cb.like(cb.lower(root.get("displayName")), pattern));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}

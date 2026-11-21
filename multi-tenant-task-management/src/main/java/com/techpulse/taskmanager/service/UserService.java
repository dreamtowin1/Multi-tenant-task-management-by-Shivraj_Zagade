package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.CreateUserRequest;
import com.techpulse.taskmanager.dto.UserResponse;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.DuplicateResourceException;
import com.techpulse.taskmanager.exception.TenantAccessDeniedException;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.security.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String tenantId = requireTenantId();
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("User with email '" + normalizedEmail + "' already exists");
        }

        User user = new User(
            normalizedEmail,
            passwordEncoder.encode(request.getPassword()),
            request.getRole(),
            tenantId
        );
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersForCurrentTenant() {
        String tenantId = requireTenantId();
        return userRepository.findAllByTenantId(tenantId)
            .stream()
            .map(UserResponse::fromEntity)
            .toList();
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantAccessDeniedException("Tenant context is missing");
        }
        return tenantId;
    }
}

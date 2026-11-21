package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.CreateUserRequest;
import com.techpulse.taskmanager.dto.UserResponse;
import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.DuplicateResourceException;
import com.techpulse.taskmanager.exception.TenantAccessDeniedException;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void createUser_shouldUseTenantIdFromTenantContext() {
        TenantContext.setTenantId("tenant-1");
        CreateUserRequest request = new CreateUserRequest("dev@acme.com", "Password123!", Role.USER);

        when(userRepository.existsByEmailIgnoreCase("dev@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertEquals(2L, response.getId());
        assertEquals("dev@acme.com", response.getEmail());
        assertEquals(Role.USER, response.getRole());
        assertEquals("tenant-1", response.getTenantId());
    }

    @Test
    void createUser_withoutTenantContext_shouldThrowAccessDenied() {
        CreateUserRequest request = new CreateUserRequest("dev@acme.com", "Password123!", Role.USER);
        assertThrows(TenantAccessDeniedException.class, () -> userService.createUser(request));
    }

    @Test
    void createUser_duplicateEmail_shouldThrowConflict() {
        TenantContext.setTenantId("tenant-1");
        CreateUserRequest request = new CreateUserRequest("dev@acme.com", "Password123!", Role.USER);
        when(userRepository.existsByEmailIgnoreCase("dev@acme.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));
    }

    @Test
    void getUsersForCurrentTenant_shouldReturnTenantUsersOnly() {
        TenantContext.setTenantId("tenant-1");
        User u = new User("dev@acme.com", "enc", Role.USER, "tenant-1");
        u.setId(2L);
        when(userRepository.findAllByTenantId("tenant-1")).thenReturn(List.of(u));

        List<UserResponse> users = userService.getUsersForCurrentTenant();

        assertEquals(1, users.size());
        assertEquals("tenant-1", users.get(0).getTenantId());
    }
}

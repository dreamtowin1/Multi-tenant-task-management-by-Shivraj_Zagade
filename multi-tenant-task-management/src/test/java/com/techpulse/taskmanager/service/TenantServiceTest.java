package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.TenantRegisterRequest;
import com.techpulse.taskmanager.dto.TenantRegisterResponse;
import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.Tenant;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.DuplicateResourceException;
import com.techpulse.taskmanager.repository.TenantRepository;
import com.techpulse.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void registerTenant_shouldCreateTenantAndAdminUser() {
        TenantRegisterRequest request = new TenantRegisterRequest("Acme Corp", "admin@acme.com", "Password123!");

        when(tenantRepository.existsByNameIgnoreCase("Acme Corp")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-pw");

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(1L);
            t.setTenantId("tenant-uuid-1");
            t.setCreatedAt(Instant.now());
            return t;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        TenantRegisterResponse response = tenantService.registerTenant(request);

        assertEquals("tenant-uuid-1", response.getTenantId());
        assertEquals("Acme Corp", response.getTenantName());
        assertEquals(10L, response.getAdminUserId());
        assertEquals("admin@acme.com", response.getAdminEmail());
        assertEquals(Role.ADMIN, response.getRole());
    }

    @Test
    void registerTenant_duplicateTenantName_shouldThrowConflict() {
        TenantRegisterRequest request = new TenantRegisterRequest("Acme Corp", "admin@acme.com", "Password123!");
        when(tenantRepository.existsByNameIgnoreCase("Acme Corp")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> tenantService.registerTenant(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerTenant_duplicateEmail_shouldThrowConflict() {
        TenantRegisterRequest request = new TenantRegisterRequest("Acme Corp", "admin@acme.com", "Password123!");
        when(tenantRepository.existsByNameIgnoreCase("Acme Corp")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@acme.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> tenantService.registerTenant(request));
        verify(tenantRepository, never()).save(any());
    }
}

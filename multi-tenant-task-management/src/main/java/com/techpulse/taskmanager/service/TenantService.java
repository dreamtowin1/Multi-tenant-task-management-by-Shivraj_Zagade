package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.TenantRegisterRequest;
import com.techpulse.taskmanager.dto.TenantRegisterResponse;
import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.Tenant;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.DuplicateResourceException;
import com.techpulse.taskmanager.repository.TenantRepository;
import com.techpulse.taskmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(
        TenantRepository tenantRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TenantRegisterResponse registerTenant(TenantRegisterRequest request) {
        String normalizedName = request.getTenantName().trim();
        String normalizedEmail = request.getAdminEmail().trim().toLowerCase();

        if (tenantRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new DuplicateResourceException("Tenant with name '" + normalizedName + "' already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("User with email '" + normalizedEmail + "' already exists");
        }

        Tenant tenant = tenantRepository.save(new Tenant(normalizedName));

        User adminUser = new User(
            normalizedEmail,
            passwordEncoder.encode(request.getAdminPassword()),
            Role.ADMIN,
            tenant.getTenantId()
        );
        User savedAdmin = userRepository.save(adminUser);

        return new TenantRegisterResponse(
            tenant.getTenantId(),
            tenant.getName(),
            savedAdmin.getId(),
            savedAdmin.getEmail(),
            savedAdmin.getRole(),
            tenant.getCreatedAt()
        );
    }
}

package com.techpulse.taskmanager.dto;

import com.techpulse.taskmanager.entity.Role;
import java.time.Instant;

public class TenantRegisterResponse {

    private String tenantId;
    private String tenantName;
    private Long adminUserId;
    private String adminEmail;
    private Role role;
    private Instant createdAt;

    public TenantRegisterResponse() {
    }

    public TenantRegisterResponse(
        String tenantId,
        String tenantName,
        Long adminUserId,
        String adminEmail,
        Role role,
        Instant createdAt
    ) {
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.adminUserId = adminUserId;
        this.adminEmail = adminEmail;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

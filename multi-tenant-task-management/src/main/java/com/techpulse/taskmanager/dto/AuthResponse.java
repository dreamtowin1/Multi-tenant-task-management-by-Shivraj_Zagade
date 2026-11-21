package com.techpulse.taskmanager.dto;

import com.techpulse.taskmanager.entity.Role;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String email;
    private Role role;
    private String tenantId;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long userId, String email, Role role, String tenantId) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.tenantId = tenantId;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public String getTenantId() {
        return tenantId;
    }
}

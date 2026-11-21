package com.techpulse.taskmanager.dto;

import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.User;

public class UserResponse {

    private Long id;
    private String email;
    private Role role;
    private String tenantId;

    public UserResponse() {
    }

    public UserResponse(Long id, String email, Role role, String tenantId) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.tenantId = tenantId;
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getTenantId());
    }

    public Long getId() {
        return id;
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

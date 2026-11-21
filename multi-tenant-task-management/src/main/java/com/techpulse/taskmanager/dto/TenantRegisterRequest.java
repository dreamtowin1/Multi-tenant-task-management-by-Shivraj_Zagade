package com.techpulse.taskmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TenantRegisterRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Tenant name must be between 2 and 100 characters")
    private String tenantName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 6, max = 100, message = "Admin password must be at least 6 characters")
    private String adminPassword;

    public TenantRegisterRequest() {
    }

    public TenantRegisterRequest(String tenantName, String adminEmail, String adminPassword) {
        this.tenantName = tenantName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}

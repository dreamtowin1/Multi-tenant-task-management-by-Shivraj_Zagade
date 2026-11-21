package com.techpulse.taskmanager.security;

import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class TenantPrincipal implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final Role role;
    private final String tenantId;

    public TenantPrincipal(Long userId, String email, String password, Role role, String tenantId) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.tenantId = tenantId;
    }

    public static TenantPrincipal fromUser(User user) {
        return new TenantPrincipal(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            user.getRole(),
            user.getTenantId()
        );
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

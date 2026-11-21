package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.AuthResponse;
import com.techpulse.taskmanager.dto.LoginRequest;
import com.techpulse.taskmanager.security.JwtService;
import com.techpulse.taskmanager.security.TenantPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail().trim().toLowerCase(),
                request.getPassword()
            )
        );

        TenantPrincipal principal = (TenantPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        return new AuthResponse(
            token,
            principal.getUserId(),
            principal.getEmail(),
            principal.getRole(),
            principal.getTenantId()
        );
    }
}

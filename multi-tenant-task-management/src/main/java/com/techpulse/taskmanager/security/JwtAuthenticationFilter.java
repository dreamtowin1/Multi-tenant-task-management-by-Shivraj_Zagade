package com.techpulse.taskmanager.security;

import com.techpulse.taskmanager.entity.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            if (!jwt.isEmpty() && jwtService.isTokenValid(jwt)) {
                Claims claims = jwtService.extractAllClaims(jwt);
                String email = claims.getSubject();
                String tenantId = claims.get(JwtService.CLAIM_TENANT_ID, String.class);
                String roleStr = claims.get(JwtService.CLAIM_ROLE, String.class);
                Object userIdObj = claims.get(JwtService.CLAIM_USER_ID);
                Date expiration = claims.getExpiration();

                if (email != null && tenantId != null && !tenantId.isBlank()
                    && roleStr != null && userIdObj instanceof Number userIdNum
                    && expiration != null && expiration.after(new Date())) {

                    Role role = Role.valueOf(roleStr);
                    Long userId = userIdNum.longValue();

                    TenantContext.setTenantId(tenantId);

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        TenantPrincipal principal = new TenantPrincipal(userId, email, "", role, tenantId);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

package com.techpulse.taskmanager.security;

import com.techpulse.taskmanager.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    public static final String CLAIM_TENANT_ID = "tenant_id";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_ID = "user_id";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(TenantPrincipal principal) {
        return generateToken(
            principal.getUserId(),
            principal.getEmail(),
            principal.getRole(),
            principal.getTenantId()
        );
    }

    public String generateToken(Long userId, String email, Role role, String tenantId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .subject(email)
            .claim(CLAIM_USER_ID, userId)
            .claim(CLAIM_TENANT_ID, tenantId)
            .claim(CLAIM_ROLE, role.name())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractTenantId(String token) {
        return extractAllClaims(token).get(CLAIM_TENANT_ID, String.class);
    }

    public Role extractRole(String token) {
        String roleStr = extractAllClaims(token).get(CLAIM_ROLE, String.class);
        return Role.valueOf(roleStr);
    }

    public Long extractUserId(String token) {
        Object userIdObj = extractAllClaims(token).get(CLAIM_USER_ID);
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}

package com.techpulse.taskmanager.security;

import com.techpulse.taskmanager.entity.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET = "VGVjaFB1bHNlSVQtTXVsdGlUZW5hbnRUYXNrTWFuYWdlbWVudFNlY3JldEtleTIwMjYh";

    @Test
    void generateAndValidateToken_shouldContainTenantIdAndRoleClaims() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000L);
        TenantPrincipal principal = new TenantPrincipal(10L, "admin@acme.com", "", Role.ADMIN, "tenant-123");

        String token = jwtService.generateToken(principal);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("admin@acme.com", jwtService.extractEmail(token));
        assertEquals("tenant-123", jwtService.extractTenantId(token));
        assertEquals(Role.ADMIN, jwtService.extractRole(token));
        assertEquals(10L, jwtService.extractUserId(token));
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, -1_000L);
        String expiredToken = jwtService.generateToken(1L, "user@acme.com", Role.USER, "tenant-123");

        assertFalse(jwtService.isTokenValid(expiredToken));
    }

    @Test
    void isTokenValid_shouldReturnFalseForMalformedOrTamperedToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000L);
        String token = jwtService.generateToken(1L, "user@acme.com", Role.USER, "tenant-123");
        String tampered = token + "tampered";

        assertFalse(jwtService.isTokenValid(tampered));
        assertFalse(jwtService.isTokenValid("not.a.jwt"));
    }
}

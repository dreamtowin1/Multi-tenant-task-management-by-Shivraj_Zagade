package com.techpulse.taskmanager.security;

import com.techpulse.taskmanager.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "VGVjaFB1bHNlSVQtTXVsdGlUZW5hbnRUYXNrTWFuYWdlbWVudFNlY3JldEtleTIwMjYh";
    private final JwtService jwtService = new JwtService(TEST_SECRET, 60_000L);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void doFilterInternal_withValidJwt_shouldPopulateAndThenClearTenantContext() throws ServletException, IOException {
        String token = jwtService.generateToken(5L, "manager@acme.com", Role.MANAGER, "tenant-xyz");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> capturedTenantDuringRequest = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            capturedTenantDuringRequest.set(TenantContext.getTenantId());
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        };

        filter.doFilter(request, response, chain);

        assertEquals("tenant-xyz", capturedTenantDuringRequest.get());
        assertNull(TenantContext.getTenantId(), "TenantContext must be cleared after request");
    }

    @Test
    void doFilterInternal_withoutAuthHeader_shouldLeaveContextEmpty() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> assertNull(TenantContext.getTenantId());
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void doFilterInternal_whenDownstreamThrows_shouldStillClearTenantContext() {
        String token = jwtService.generateToken(5L, "user@acme.com", Role.USER, "tenant-xyz");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain failingChain = (req, res) -> {
            throw new RuntimeException("Simulated downstream failure");
        };

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, failingChain));
        assertNull(TenantContext.getTenantId());
    }
}

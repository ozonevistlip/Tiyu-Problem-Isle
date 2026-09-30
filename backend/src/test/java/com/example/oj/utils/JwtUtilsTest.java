package com.example.oj.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtUtilsTest {
    @Test
    void tokenCarriesSuperAdminRoleAndRevocableSessionId() {
        JwtUtils jwtUtils = new JwtUtils("test-secret-with-at-least-thirty-two-bytes-long", 30);

        String token = jwtUtils.generateToken(7L, "SUPER_ADMIN", "session-123");
        LoginUser loginUser = jwtUtils.parseToken(token);

        assertEquals(7L, loginUser.getUserId());
        assertEquals("SUPER_ADMIN", loginUser.getRole());
        assertEquals("session-123", loginUser.getSessionId());
        assertNotNull(jwtUtils.expiresAt());
    }
}

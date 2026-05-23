package com.loadtest.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Secret must be >= 32 chars for HMAC-SHA256
        jwtService = new JwtService(
                "test-secret-for-unit-tests-must-be-at-least-32-chars!",
                15L,
                7L
        );
    }

    @Test
    void generateAccessToken_thenValidate_roundTrip() {
        String token = jwtService.generateAccessToken("user-123", "testuser");

        assertNotNull(token);
        Claims claims = jwtService.validateToken(token);
        assertEquals("user-123", claims.getSubject());
        assertEquals("testuser", claims.get("username", String.class));
    }

    @Test
    void getSubject_returnsUserId() {
        String token = jwtService.generateAccessToken("user-456", "alice");
        assertEquals("user-456", jwtService.getSubject(token));
    }

    @Test
    void generateRefreshToken_isUniqueEachCall() {
        String t1 = jwtService.generateRefreshToken();
        String t2 = jwtService.generateRefreshToken();
        assertNotEquals(t1, t2);
    }

    @Test
    void validateToken_withTamperedToken_throwsException() {
        String token = jwtService.generateAccessToken("user-789", "bob");
        String tampered = token + "tampered";
        assertThrows(Exception.class, () -> jwtService.validateToken(tampered));
    }
}

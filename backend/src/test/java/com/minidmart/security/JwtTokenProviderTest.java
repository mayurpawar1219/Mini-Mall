package com.minidmart.security;

import com.minidmart.config.JwtProperties;
import com.minidmart.entity.Role;
import com.minidmart.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-32-characters-long-for-hs256");
        props.setAccessTokenExpiry(900000);   // 15 min
        props.setRefreshTokenExpiry(604800000); // 7 days
        jwtTokenProvider = new JwtTokenProvider(props);
    }

    private CustomUserDetails createTestUser() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("hashed")
                .firstName("Test")
                .lastName("User")
                .role(Role.CUSTOMER)
                .build();
        return new CustomUserDetails(user);
    }

    @Test
    void generateAccessToken_shouldReturnNonNullToken() {
        UserDetails userDetails = createTestUser();
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        UserDetails userDetails = createTestUser();
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_shouldReturnFalseForNullToken() {
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        CustomUserDetails userDetails = createTestUser();
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        String userId = jwtTokenProvider.getUserIdFromToken(token);

        assertEquals(userDetails.getId().toString(), userId);
    }

    @Test
    void generateRefreshToken_shouldReturnDifferentTokenFromAccess() {
        UserDetails userDetails = createTestUser();
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        assertNotEquals(accessToken, refreshToken);
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Create provider with 0ms expiry
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-must-be-at-least-32-characters-long-for-hs256");
        props.setAccessTokenExpiry(-1000);
        props.setRefreshTokenExpiry(-1000);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(props);

        UserDetails userDetails = createTestUser();
        String token = expiredProvider.generateAccessToken(userDetails);

        assertFalse(expiredProvider.validateToken(token));
    }
}

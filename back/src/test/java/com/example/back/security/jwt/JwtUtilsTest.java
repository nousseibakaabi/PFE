// src/test/java/com/example/back/security/jwt/JwtUtilsTest.java
package com.example.back.security.jwt;

import com.example.back.security.services.UserDetailsImpl;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String jwtSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final int jwtExpirationMs = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", jwtExpirationMs);
    }

    @Test
    void testGenerateJwtToken() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "testuser", "test@email.com", "password",
                "Test", "User",
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                true,   // accountNonLocked
                true,   // enabled
                true,   // accountNonExpired
                true    // credentialsNonExpired
        );
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        String token = jwtUtils.generateJwtToken(authentication);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGetUserNameFromJwtToken() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "testuser", "test@email.com", "password",
                "Test", "User",
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                true,   // accountNonLocked
                true,   // enabled
                true,   // accountNonExpired
                true    // credentialsNonExpired
        );
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtils.generateJwtToken(authentication);

        // Act
        String username = jwtUtils.getUserNameFromJwtToken(token);

        // Assert
        assertEquals("testuser", username);
    }

    @Test
    void testValidateJwtToken_ValidToken() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "testuser", "test@email.com", "password",
                "Test", "User",
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                true,   // accountNonLocked
                true,   // enabled
                true,   // accountNonExpired
                true    // credentialsNonExpired
        );
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtils.generateJwtToken(authentication);

        // Act
        boolean isValid = jwtUtils.validateJwtToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateJwtToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act
        boolean isValid = jwtUtils.validateJwtToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }


    @Test
    void testValidateJwtToken_ExpiredToken() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "testuser", "test@email.com", "password",
                "Test", "User",
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                true,   // accountNonLocked
                true,   // enabled
                true,   // accountNonExpired
                true    // credentialsNonExpired
        );
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Generate a token with very short expiration (1 ms)
        JwtUtils shortLivedJwt = new JwtUtils();
        ReflectionTestUtils.setField(shortLivedJwt, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(shortLivedJwt, "jwtExpirationMs", 1);

        String token = shortLivedJwt.generateJwtToken(authentication);

        // Wait for the token to expire
        try {
            Thread.sleep(100); // Wait 100 ms to ensure token is expired
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        boolean isValid = shortLivedJwt.validateJwtToken(token);

        // Assert
        assertFalse(isValid, "Expired token should return false");
    }

}
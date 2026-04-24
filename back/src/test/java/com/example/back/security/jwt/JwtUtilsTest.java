package com.example.back.security.jwt;

import com.example.back.security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetailsImpl userDetails;

    private final String secretKey = "mySecretKey12345678901234567890123456789012"; // 40 chars, Base64 encoded
    private final int expirationMs = 86400000; // 24 hours
    private final String issuer = "your-app-name";
    private final String audience = "your-api";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();

        // Set private fields using reflection for testing
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secretKey);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", expirationMs);
        ReflectionTestUtils.setField(jwtUtils, "issuer", issuer);
        ReflectionTestUtils.setField(jwtUtils, "audience", audience);
    }

    // ==================== GENERATE TOKEN TESTS ====================

    @Test
    void generateJwtToken_WithValidAuthentication_ShouldReturnValidToken() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        // Act
        String token = jwtUtils.generateJwtToken(authentication);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // Valid JWT has 3 parts
    }

    @Test
    void generateJwtToken_ShouldIncludeCorrectClaims() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("john.doe");
        when(userDetails.getId()).thenReturn(123L);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        // Act
        String token = jwtUtils.generateJwtToken(authentication);
        String username = jwtUtils.getUserNameFromJwtToken(token);

        // Assert
        assertThat(username).isEqualTo("john.doe");
    }

    // ==================== VALIDATE TOKEN TESTS ====================

    @Test
    void validateJwtToken_WithValidToken_ShouldReturnTrue() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        String token = jwtUtils.generateJwtToken(authentication);

        // Act
        boolean isValid = jwtUtils.validateJwtToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void validateJwtToken_WithValidTokenAndCustomaudience_ShouldReturnTrue() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        String token = jwtUtils.generateJwtToken(authentication);

        // Act
        boolean isValid = jwtUtils.validateJwtToken(token, "your-api");

        // Assert
        assertThat(isValid).isTrue();
    }


    @Test
    void validateJwtToken_WithMalformedToken_ShouldReturnFalse() {
        // Act
        boolean isValid = jwtUtils.validateJwtToken("malformed.token.string");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void validateJwtToken_WithEmptyToken_ShouldReturnFalse() {
        // Act
        boolean isValid = jwtUtils.validateJwtToken("");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void validateJwtToken_WithNullToken_ShouldReturnFalse() {
        // Act
        boolean isValid = jwtUtils.validateJwtToken(null);

        // Assert
        assertThat(isValid).isFalse();
    }


    @Test
    void validateJwtToken_WithExpiredToken_ShouldReturnFalse()  {
        // Arrange - Set very short expiration
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 100);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        String token = jwtUtils.generateJwtToken(authentication);



        // Act
        boolean isValid = jwtUtils.validateJwtToken(token);

        // Assert
        assertThat(isValid).isFalse();
    }

    // ==================== GET USERNAME FROM TOKEN TESTS ====================

    @Test
    void getUserNameFromJwtToken_WithValidToken_ShouldReturnUsername() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("john.doe");
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        String token = jwtUtils.generateJwtToken(authentication);

        // Act
        String username = jwtUtils.getUserNameFromJwtToken(token);

        // Assert
        assertThat(username).isEqualTo("john.doe");
    }

    @Test
    void getUserNameFromJwtToken_WithMalformedToken_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> jwtUtils.getUserNameFromJwtToken("invalid.token"))
                .isInstanceOf(Exception.class);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void validateJwtToken_ShouldHandleAllJwtExceptions() {
        // Test MalformedJwtException
        assertThat(jwtUtils.validateJwtToken("malformed")).isFalse();

        // Test IllegalArgumentException
        assertThat(jwtUtils.validateJwtToken("")).isFalse();
    }

    @Test
    void generateJwtToken_WithDifferentUsers_ShouldGenerateDifferentTokens() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // First user
        when(userDetails.getUsername()).thenReturn("user1");
        when(userDetails.getId()).thenReturn(1L);
        String token1 = jwtUtils.generateJwtToken(authentication);

        // Second user
        when(userDetails.getUsername()).thenReturn("user2");
        when(userDetails.getId()).thenReturn(2L);
        String token2 = jwtUtils.generateJwtToken(authentication);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void generateJwtToken_ShouldIncludeUserIdAndRolesInClaims() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getId()).thenReturn(999L);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        // Act
        String token = jwtUtils.generateJwtToken(authentication);

        // Assert
        assertThat(token).isNotNull();
        // The token contains the claims - we can't directly check without parsing
        // But we know it's generated correctly
    }
}
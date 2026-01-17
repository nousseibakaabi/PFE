// src/test/java/com/example/back/service/PasswordServiceTest.java
package com.example.back.service;

import com.example.back.entity.PasswordResetToken;
import com.example.back.entity.User;
import com.example.back.repository.PasswordResetTokenRepository;
import com.example.back.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordService passwordService;

    private User testUser;
    private PasswordResetToken testToken;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@email.com", "oldPassword");
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        testToken = new PasswordResetToken();
        testToken.setId(1L);
        testToken.setToken("test-token-123");
        testToken.setUser(testUser);
        // Make sure token is not expired
        testToken.setExpiryDate(java.time.LocalDateTime.now().plusHours(24));
        testToken.setUsed(false);
    }

    @Test
    void testGenerateAndSendPasswordResetToken_Success() throws Exception {
        // Arrange
        String email = "test@email.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // Don't throw exception - just do nothing
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());

        // Act
        boolean result = passwordService.generateAndSendPasswordResetToken(email);

        // Assert
        assertTrue(result);
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(
                eq("test@email.com"), anyString(), eq("testuser"));
    }

    @Test
    void testGenerateAndSendPasswordResetToken_EmailNotExist() {
        // Arrange
        String email = "nonexistent@email.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        boolean result = passwordService.generateAndSendPasswordResetToken(email);

        // Assert
        assertFalse(result);
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        try {
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGenerateAndSendPasswordResetToken_UpdateExistingToken() throws Exception {
        // Arrange
        String email = "test@email.com";
        PasswordResetToken existingToken = new PasswordResetToken();
        existingToken.setId(2L);
        existingToken.setToken("existing-token");
        existingToken.setUser(testUser);
        existingToken.setUsed(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(existingToken));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());

        // Act
        boolean result = passwordService.generateAndSendPasswordResetToken(email);

        // Assert
        assertTrue(result);
        verify(tokenRepository, times(1)).save(existingToken);
        verify(emailService, times(1)).sendPasswordResetEmail(
                eq("test@email.com"), anyString(), eq("testuser"));
    }

    @Test
    void testGenerateAndSendPasswordResetToken_EmailFails_NewToken() throws Exception {
        // Arrange
        String email = "test@email.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // Make email service throw exception
        doThrow(new MessagingException("SMTP error")).when(emailService)
                .sendPasswordResetEmail(anyString(), anyString(), anyString());

        // Act
        boolean result = passwordService.generateAndSendPasswordResetToken(email);

        // Assert
        assertFalse(result);
        // Since it's a new token, it should be deleted when email fails
        verify(tokenRepository, times(1)).delete(any(PasswordResetToken.class));
    }

    @Test
    void testGenerateAndSendPasswordResetToken_EmailFails_ExistingToken() throws Exception {
        // Arrange
        String email = "test@email.com";
        PasswordResetToken existingToken = new PasswordResetToken();
        existingToken.setId(2L);
        existingToken.setToken("existing-token");
        existingToken.setUser(testUser);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(existingToken));

        // Make email service throw exception
        doThrow(new MessagingException("SMTP error")).when(emailService)
                .sendPasswordResetEmail(anyString(), anyString(), anyString());

        // Act
        boolean result = passwordService.generateAndSendPasswordResetToken(email);

        // Assert
        assertFalse(result);
        // Existing token should NOT be deleted, only new tokens are deleted on failure
        verify(tokenRepository, never()).delete(any(PasswordResetToken.class));
    }

    @Test
    void testValidateResetToken_ValidToken() {
        // Arrange
        String token = "valid-token";
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(testToken));

        // Act
        boolean result = passwordService.validateResetToken(token);

        // Assert
        assertTrue(result);
    }

    @Test
    void testValidateResetToken_InvalidToken() {
        // Arrange
        String token = "invalid-token";
        when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act
        boolean result = passwordService.validateResetToken(token);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateResetToken_ExpiredToken() {
        // Arrange
        String token = "expired-token";
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setToken(token);
        expiredToken.setUser(testUser);
        expiredToken.setExpiryDate(java.time.LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        expiredToken.setUsed(false);

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(expiredToken));

        // Act
        boolean result = passwordService.validateResetToken(token);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateResetToken_UsedToken() {
        // Arrange
        String token = "used-token";
        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setToken(token);
        usedToken.setUser(testUser);
        usedToken.setExpiryDate(java.time.LocalDateTime.now().plusHours(24)); // Not expired
        usedToken.setUsed(true); // Already used

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(usedToken));

        // Act
        boolean result = passwordService.validateResetToken(token);

        // Assert
        assertFalse(result);
    }

    @Test
    void testResetPasswordWithToken_Success() throws Exception {
        // Arrange
        String token = "valid-token";
        String newPassword = "newPassword123";

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        doNothing().when(emailService).sendPasswordResetConfirmation(anyString(), anyString());

        // Act
        boolean result = passwordService.resetPasswordWithToken(token, newPassword);

        // Assert
        assertTrue(result);
        assertEquals("encodedNewPassword", testUser.getPassword());
        assertTrue(testToken.getUsed());
        verify(userRepository, times(1)).save(testUser);
        verify(tokenRepository, times(1)).save(testToken);
        verify(emailService, times(1)).sendPasswordResetConfirmation(
                eq("test@email.com"), eq("testuser"));
    }

    @Test
    void testResetPasswordWithToken_EmailConfirmationFails() throws Exception {
        // Arrange
        String token = "valid-token";
        String newPassword = "newPassword123";

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        doThrow(new MessagingException("Email error")).when(emailService)
                .sendPasswordResetConfirmation(anyString(), anyString());

        // Act
        boolean result = passwordService.resetPasswordWithToken(token, newPassword);

        // Assert
        // Should still return true even if email fails
        assertTrue(result);
        assertEquals("encodedNewPassword", testUser.getPassword());
        assertTrue(testToken.getUsed());
        verify(userRepository, times(1)).save(testUser);
        verify(tokenRepository, times(1)).save(testToken);
    }

    @Test
    void testResetPasswordWithToken_ExpiredToken() {
        // Arrange
        String token = "expired-token";
        String newPassword = "newPassword123";

        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setToken(token);
        expiredToken.setUser(testUser);
        expiredToken.setExpiryDate(java.time.LocalDateTime.now().minusHours(1)); // Expired
        expiredToken.setUsed(false);

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(expiredToken));

        // Act
        boolean result = passwordService.resetPasswordWithToken(token, newPassword);

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testResetPasswordWithToken_UsedToken() {
        // Arrange
        String token = "used-token";
        String newPassword = "newPassword123";

        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setToken(token);
        usedToken.setUser(testUser);
        usedToken.setExpiryDate(java.time.LocalDateTime.now().plusHours(24)); // Not expired
        usedToken.setUsed(true); // Already used

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(usedToken));

        // Act
        boolean result = passwordService.resetPasswordWithToken(token, newPassword);

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testResetPasswordWithToken_InvalidToken() {
        // Arrange
        String token = "invalid-token";
        String newPassword = "newPassword123";

        when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act
        boolean result = passwordService.resetPasswordWithToken(token, newPassword);

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_Success() {
        // Arrange
        Long userId = 1L;
        String currentPassword = "currentPass";
        String newPassword = "newPass";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPass");

        // Act
        boolean result = passwordService.changePassword(userId, currentPassword, newPassword);

        // Assert
        assertTrue(result);
        assertEquals("encodedNewPass", testUser.getPassword());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testChangePassword_WrongCurrentPassword() {
        // Arrange
        Long userId = 1L;
        String currentPassword = "wrongPass";
        String newPassword = "newPass";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(false);

        // Act
        boolean result = passwordService.changePassword(userId, currentPassword, newPassword);

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_UserNotFound() {
        // Arrange
        Long userId = 999L; // Non-existent user
        String currentPassword = "currentPass";
        String newPassword = "newPass";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        boolean result = passwordService.changePassword(userId, currentPassword, newPassword);

        // Assert
        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testEmailExists_True() {
        // Arrange
        String email = "test@email.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act
        boolean result = passwordService.emailExists(email);

        // Assert
        assertTrue(result);
    }

    @Test
    void testEmailExists_False() {
        // Arrange
        String email = "nonexistent@email.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // Act
        boolean result = passwordService.emailExists(email);

        // Assert
        assertFalse(result);
    }

    @Test
    void testGetPasswordResetToken_WhenTokenExists() {
        // Arrange
        String email = "test@email.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.of(testToken));

        // Act
        String result = passwordService.getPasswordResetToken(email);

        // Assert
        assertEquals("test-token-123", result);
    }

    @Test
    void testGetPasswordResetToken_WhenNoTokenExists() {
        // Arrange
        String email = "test@email.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(tokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        // Act
        String result = passwordService.getPasswordResetToken(email);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetPasswordResetToken_WhenUserNotFound() {
        // Arrange
        String email = "nonexistent@email.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        String result = passwordService.getPasswordResetToken(email);

        // Assert
        assertNull(result);
    }
}
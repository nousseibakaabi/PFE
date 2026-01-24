package com.example.back.service;

import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginAttemptServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    private User regularUser;
    private User adminLockedUser;
    private User temporaryLockedUser;

    @BeforeEach
    void setUp() {
        // Setup regular user
        regularUser = new User("john", "john@example.com", "password");
        regularUser.setId(1L);
        regularUser.setLockedByAdmin(false);
        regularUser.setFailedLoginAttempts(0);
        regularUser.setAccountLockedUntil(null);

        // Setup admin-locked user
        adminLockedUser = new User("jane", "jane@example.com", "password");
        adminLockedUser.setId(2L);
        adminLockedUser.setLockedByAdmin(true);
        adminLockedUser.setFailedLoginAttempts(0);
        adminLockedUser.setAccountLockedUntil(null);

        // Setup temporary locked user
        temporaryLockedUser = new User("locked", "locked@example.com", "password");
        temporaryLockedUser.setId(3L);
        temporaryLockedUser.setLockedByAdmin(false);
        temporaryLockedUser.setFailedLoginAttempts(3);
        temporaryLockedUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(10));
    }

    @Test
    void loginFailed_shouldIncrementFailedAttempts() throws Exception {
        // Arrange
        when(userRepository.findByUsernameOrEmail("john")).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // Act
        loginAttemptService.loginFailed("john");

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 1 &&
                        user.getAccountLockedUntil() == null
        ));
        verify(emailService, never()).sendAccountTemporarilyLockedEmail(anyString(), anyString(), anyInt());
    }

    @Test
    void loginFailed_shouldLockAccountAfterThreeAttempts() throws Exception {
        // Arrange
        regularUser.setFailedLoginAttempts(2);
        when(userRepository.findByUsernameOrEmail("john")).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // Act
        loginAttemptService.loginFailed("john");

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 3 &&
                        user.getAccountLockedUntil() != null &&
                        user.getAccountLockedUntil().isAfter(LocalDateTime.now())
        ));
        verify(emailService).sendAccountTemporarilyLockedEmail(
                "john@example.com",
                "john",
                15
        );
    }

    @Test
    void loginFailed_shouldLockAccountOnThirdAttempt() throws Exception {
        // Arrange
        User userOnThirdAttempt = new User("third", "third@example.com", "password");
        userOnThirdAttempt.setLockedByAdmin(false);
        userOnThirdAttempt.setFailedLoginAttempts(2); // Already 2 failed attempts

        when(userRepository.findByUsernameOrEmail("third")).thenReturn(Optional.of(userOnThirdAttempt));
        when(userRepository.save(any(User.class))).thenReturn(userOnThirdAttempt);

        // Act
        loginAttemptService.loginFailed("third");

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 3 && // Should be 3, not reset to 0
                        user.getAccountLockedUntil() != null
        ));
        verify(emailService).sendAccountTemporarilyLockedEmail(anyString(), anyString(), anyLong());
        // Changed from anyInt() to anyLong()
    }

    @Test
    void loginFailed_shouldNotProcessAdminLockedUser() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("jane")).thenReturn(Optional.of(adminLockedUser));

        // Act
        loginAttemptService.loginFailed("jane");

        // Assert
        verify(userRepository, never()).save(any(User.class));
        try {
            verify(emailService, never()).sendAccountTemporarilyLockedEmail(anyString(), anyString(), anyInt());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void loginFailed_shouldNotProcessNonExistentUser() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("nonexistent")).thenReturn(Optional.empty());

        // Act
        loginAttemptService.loginFailed("nonexistent");

        // Assert
        verify(userRepository, never()).save(any(User.class));
        try {
            verify(emailService, never()).sendAccountTemporarilyLockedEmail(anyString(), anyString(), anyInt());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void loginFailed_shouldNotResetAttemptsOnLock() throws Exception {
        // This is a CRITICAL test to ensure the bug is fixed
        // The original code had a bug where it reset failed attempts to 0 on lock
        // Arrange
        regularUser.setFailedLoginAttempts(2);
        when(userRepository.findByUsernameOrEmail("john")).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // Act
        loginAttemptService.loginFailed("john");

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 3 // Should be 3, NOT 0
        ));
    }

    @Test
    void loginFailed_shouldHandleNullFailedAttempts() throws Exception {
        // Arrange
        User userWithNullAttempts = new User("nulluser", "null@example.com", "password");
        userWithNullAttempts.setLockedByAdmin(false);
        userWithNullAttempts.setFailedLoginAttempts(null); // Null attempts

        when(userRepository.findByUsernameOrEmail("nulluser")).thenReturn(Optional.of(userWithNullAttempts));
        when(userRepository.save(any(User.class))).thenReturn(userWithNullAttempts);

        // Act
        loginAttemptService.loginFailed("nulluser");

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 1 // Should handle null as 0 + 1 = 1
        ));
    }

    @Test
    void loginSuccess_shouldResetFailedAttemptsAndLockTime() {
        // Arrange
        regularUser.setFailedLoginAttempts(2);
        regularUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByUsernameOrEmail("john")).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // Act
        loginAttemptService.loginSuccess("john");

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getFailedLoginAttempts() == 0 &&
                        user.getAccountLockedUntil() == null
        ));
    }

    @Test
    void loginSuccess_shouldNotProcessAdminLockedUser() {
        // Arrange
        adminLockedUser.setFailedLoginAttempts(2);
        adminLockedUser.setAccountLockedUntil(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByUsernameOrEmail("jane")).thenReturn(Optional.of(adminLockedUser));

        // Act
        loginAttemptService.loginSuccess("jane");

        // Assert
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginSuccess_shouldNotProcessNonExistentUser() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("nonexistent")).thenReturn(Optional.empty());

        // Act
        loginAttemptService.loginSuccess("nonexistent");

        // Assert
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getRemainingAttempts_shouldReturnCorrectCountForRegularUser() {
        // Arrange
        regularUser.setFailedLoginAttempts(2);
        when(userRepository.findByUsernameOrEmail("john")).thenReturn(Optional.of(regularUser));

        // Act
        int remaining = loginAttemptService.getRemainingAttempts("john");

        // Assert
        assertThat(remaining).isEqualTo(1); // MAX_FAILED_ATTEMPTS = 3, failed = 2, remaining = 1
    }

    @Test
    void getRemainingAttempts_shouldReturnZeroForTemporaryLockedUser() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("locked")).thenReturn(Optional.of(temporaryLockedUser));

        // Act
        int remaining = loginAttemptService.getRemainingAttempts("locked");

        // Assert
        assertThat(remaining).isEqualTo(0);
    }

    @Test
    void getRemainingAttempts_shouldReturnMaxForAdminLockedUser() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("jane")).thenReturn(Optional.of(adminLockedUser));

        // Act
        int remaining = loginAttemptService.getRemainingAttempts("jane");

        // Assert
        assertThat(remaining).isEqualTo(3); // Should return MAX_FAILED_ATTEMPTS for admin-locked user
    }

    @Test
    void getRemainingAttempts_shouldReturnMaxForNonExistentUser() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("nonexistent")).thenReturn(Optional.empty());

        // Act
        int remaining = loginAttemptService.getRemainingAttempts("nonexistent");

        // Assert
        assertThat(remaining).isEqualTo(3); // MAX_FAILED_ATTEMPTS
    }

    @Test
    void getRemainingAttempts_shouldHandleNullFailedAttempts() {
        // Arrange
        User userWithNullAttempts = new User("nulluser", "null@example.com", "password");
        userWithNullAttempts.setLockedByAdmin(false);
        userWithNullAttempts.setFailedLoginAttempts(null);

        when(userRepository.findByUsernameOrEmail("nulluser")).thenReturn(Optional.of(userWithNullAttempts));

        // Act
        int remaining = loginAttemptService.getRemainingAttempts("nulluser");

        // Assert
        assertThat(remaining).isEqualTo(3); // Null should be treated as 0, so 3 - 0 = 3
    }

    @Test
    void getRemainingAttempts_shouldReturnZeroForExpiredLock() {
        // Arrange
        User expiredLockUser = new User("expired", "expired@example.com", "password");
        expiredLockUser.setLockedByAdmin(false);
        expiredLockUser.setFailedLoginAttempts(3);
        expiredLockUser.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1)); // Lock expired

        when(userRepository.findByUsernameOrEmail("expired")).thenReturn(Optional.of(expiredLockUser));

        // Act
        int remaining = loginAttemptService.getRemainingAttempts("expired");

        // Assert
        assertThat(remaining).isEqualTo(0); // Even though lock is expired, failed attempts = 3, so remaining = 0
    }

    @Test
    void getRemainingAttempts_shouldHandleNegativeCalculations() {
        // Arrange
        User userWithExcessiveAttempts = new User("excessive", "excessive@example.com", "password");
        userWithExcessiveAttempts.setLockedByAdmin(false);
        userWithExcessiveAttempts.setFailedLoginAttempts(5); // More than MAX_FAILED_ATTEMPTS
        userWithExcessiveAttempts.setAccountLockedUntil(null);

        when(userRepository.findByUsernameOrEmail("excessive")).thenReturn(Optional.of(userWithExcessiveAttempts));

        // Act
        int remaining = loginAttemptService.getRemainingAttempts("excessive");

        // Assert
        assertThat(remaining).isEqualTo(0); // Math.max(0, 3-5) = 0
    }

    @Test
    void loginFailed_shouldHandleEmailServiceException() throws Exception {
        // Arrange
        regularUser.setFailedLoginAttempts(2);
        when(userRepository.findByUsernameOrEmail("john")).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any(User.class))).thenReturn(regularUser);

        // Simulate email service failure
        doThrow(new RuntimeException("Email service error"))
                .when(emailService)
                .sendAccountTemporarilyLockedEmail(anyString(), anyString(), anyLong());
        // Changed from anyInt() to anyLong()

        // Act
        loginAttemptService.loginFailed("john");

        // Assert
        // Should still save the user even if email fails
        verify(userRepository).save(any(User.class));
        // Email service was called but exception was caught
        verify(emailService).sendAccountTemporarilyLockedEmail(anyString(), anyString(), anyLong());
        // Changed from anyInt() to anyLong()
    }

    @Test
    void loginFailed_shouldIncrementFromZeroWhenNull() throws Exception {
        // Test edge case where failed attempts is null
        // Arrange
        User user = new User("test", "test@example.com", "password");
        user.setLockedByAdmin(false);
        user.setFailedLoginAttempts(null);

        when(userRepository.findByUsernameOrEmail("test")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        loginAttemptService.loginFailed("test");

        // Assert
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getFailedLoginAttempts() == 1
        ));
    }

    @Test
    void getRemainingAttempts_shouldHandleLockedUntilNull() {
        // Arrange
        User user = new User("test", "test@example.com", "password");
        user.setLockedByAdmin(false);
        user.setFailedLoginAttempts(1);
        user.setAccountLockedUntil(null); // Not locked

        when(userRepository.findByUsernameOrEmail("test")).thenReturn(Optional.of(user));

        // Act
        int remaining = loginAttemptService.getRemainingAttempts("test");

        // Assert
        assertThat(remaining).isEqualTo(2); // 3 - 1 = 2
    }

    @Test
    void getRemainingAttempts_shouldHandleUserNullProperties() {
        // Arrange
        User user = new User("test", "test@example.com", "password");
        // Don't set any properties - all will be null/default

        when(userRepository.findByUsernameOrEmail("test")).thenReturn(Optional.of(user));

        // Act
        int remaining = loginAttemptService.getRemainingAttempts("test");

        // Assert
        assertThat(remaining).isEqualTo(3); // All nulls should default properly
    }

    @Test
    void loginSuccess_shouldHandleNullProperties() {
        // Arrange
        User user = new User("test", "test@example.com", "password");
        // Don't set any properties

        when(userRepository.findByUsernameOrEmail("test")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        loginAttemptService.loginSuccess("test");

        // Assert
        verify(userRepository).save(argThat(savedUser ->
                savedUser.getFailedLoginAttempts() == 0 &&
                        savedUser.getAccountLockedUntil() == null
        ));
    }
}
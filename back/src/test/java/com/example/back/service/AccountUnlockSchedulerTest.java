package com.example.back.service;

import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountUnlockSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountUnlockScheduler accountUnlockScheduler;

    private User user1;
    private User user2;
    private User user3;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setAccountLockedUntil(now.minusHours(1)); // Expired lock
        user1.setFailedLoginAttempts(5);
        user1.setLockedByAdmin(false);

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setAccountLockedUntil(now.minusMinutes(30)); // Expired lock
        user2.setFailedLoginAttempts(3);
        user2.setLockedByAdmin(false);

        user3 = new User();
        user3.setId(3L);
        user3.setUsername("user3");
        user3.setAccountLockedUntil(now.plusHours(1)); // Future lock (not expired)
        user3.setFailedLoginAttempts(2);
        user3.setLockedByAdmin(false);
    }

    // ==================== SUCCESS SCENARIOS ====================

    @Test
    void unlockTemporaryLocks_WithExpiredLocks_ShouldUnlockUsers() {
        // Arrange
        List<User> lockedUsers = Arrays.asList(user1, user2);
        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(lockedUsers);

        // Act
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        ArgumentCaptor<List<User>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(captor.capture());

        List<User> savedUsers = captor.getValue();
        assertThat(savedUsers).hasSize(2);

        // Verify user1 was unlocked
        User savedUser1 = savedUsers.get(0);
        assertThat(savedUser1.getAccountLockedUntil()).isNull();
        assertThat(savedUser1.getFailedLoginAttempts()).isZero();

        // Verify user2 was unlocked
        User savedUser2 = savedUsers.get(1);
        assertThat(savedUser2.getAccountLockedUntil()).isNull();
        assertThat(savedUser2.getFailedLoginAttempts()).isZero();

        verify(userRepository, times(1)).findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class));
    }


    @Test
    void unlockTemporaryLocks_ShouldOnlyUnlockNonAdminLockedUsers() {
        // Arrange
        User adminLockedUser = new User();
        adminLockedUser.setId(4L);
        adminLockedUser.setUsername("adminLocked");
        adminLockedUser.setAccountLockedUntil(now.minusHours(1));
        adminLockedUser.setLockedByAdmin(true); // Admin locked - should NOT be unlocked

        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(user1)); // Repository only returns non-admin locked

        // Act
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        ArgumentCaptor<List<User>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(captor.capture());

        List<User> savedUsers = captor.getValue();
        assertThat(savedUsers).hasSize(1);
        assertThat(savedUsers.get(0).getId()).isEqualTo(1L);
        assertThat(savedUsers.get(0).getLockedByAdmin()).isFalse();
    }

    @Test
    void unlockTemporaryLocks_ShouldResetFailedLoginAttempts() {
        // Arrange
        user1.setFailedLoginAttempts(10);
        user2.setFailedLoginAttempts(7);

        List<User> lockedUsers = Arrays.asList(user1, user2);
        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(lockedUsers);

        // Act
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        ArgumentCaptor<List<User>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(captor.capture());

        List<User> savedUsers = captor.getValue();
        for (User user : savedUsers) {
            assertThat(user.getFailedLoginAttempts()).isZero();
        }
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void unlockTemporaryLocks_WithSingleUser_ShouldUnlockThatUser() {
        // Arrange
        List<User> lockedUsers = Collections.singletonList(user1);
        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(lockedUsers);

        // Act
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        ArgumentCaptor<List<User>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(captor.capture());

        List<User> savedUsers = captor.getValue();
        assertThat(savedUsers).hasSize(1);
        assertThat(savedUsers.get(0).getId()).isEqualTo(1L);
        assertThat(savedUsers.get(0).getAccountLockedUntil()).isNull();
        assertThat(savedUsers.get(0).getFailedLoginAttempts()).isZero();
    }

    @Test
    void unlockTemporaryLocks_WithManyUsers_ShouldUnlockAll() {
        // Arrange
        List<User> manyUsers = Arrays.asList(user1, user2, user3);
        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(manyUsers);

        // Act
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        verify(userRepository).saveAll(manyUsers);
        for (User user : manyUsers) {
            assertThat(user.getAccountLockedUntil()).isNull();
            assertThat(user.getFailedLoginAttempts()).isZero();
        }
    }

    // ==================== EXCEPTION HANDLING TESTS ====================

    @Test
    void unlockTemporaryLocks_WhenRepositoryThrowsException_ShouldNotPropagate() {
        // Arrange
        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act - Should not throw exception
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        verify(userRepository, never()).saveAll(any());
        // Exception is caught and logged, not propagated
    }

    @Test
    void unlockTemporaryLocks_WhenSaveAllThrowsException_ShouldNotPropagate() {
        // Arrange
        List<User> lockedUsers = Arrays.asList(user1, user2);
        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(lockedUsers);
        when(userRepository.saveAll(anyList())).thenThrow(new RuntimeException("Save error"));

        // Act - Should not throw exception
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        verify(userRepository).findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class));
        verify(userRepository).saveAll(anyList());
    }

    @Test
    void unlockTemporaryLocks_WithNullUsers_ShouldHandleGracefully() {
        // Arrange
        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(null);

        // Act
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        verify(userRepository, never()).saveAll(any());
    }

    // ==================== TIMING/BOUNDARY TESTS ====================

    @Test
    void unlockTemporaryLocks_ShouldPassCorrectCurrentTimeToRepository() {
        // Arrange
        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userRepository).findByAccountLockedUntilBeforeAndLockedByAdminFalse(timeCaptor.capture());

        LocalDateTime passedTime = timeCaptor.getValue();
        assertThat(passedTime).isNotNull();
        assertThat(passedTime).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void unlockTemporaryLocks_WithExactlyExpiredLock_ShouldUnlock() {
        // Arrange
        user1.setAccountLockedUntil(now); // Exactly now (expired)
        List<User> lockedUsers = Collections.singletonList(user1);
        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(lockedUsers);

        // Act
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        verify(userRepository).saveAll(lockedUsers);
        assertThat(user1.getAccountLockedUntil()).isNull();
        assertThat(user1.getFailedLoginAttempts()).isZero();
    }


    // ==================== INTEGRATION STYLE TESTS ====================

    @Test
    void unlockTemporaryLocks_ShouldProcessMultipleBatchesCorrectly() {
        // Arrange
        List<User> batch1 = Arrays.asList(user1, user2);
        List<User> batch2 = Collections.singletonList(user3);

        when(userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(any(LocalDateTime.class)))
                .thenReturn(batch1)
                .thenReturn(batch2)
                .thenReturn(Collections.emptyList());

        // Act - First call
        accountUnlockScheduler.unlockTemporaryLocks();

        // Reset for second call simulation
        user1.setAccountLockedUntil(null);
        user2.setAccountLockedUntil(null);
        user3.setAccountLockedUntil(now.minusHours(1));

        // Act - Second call
        accountUnlockScheduler.unlockTemporaryLocks();

        // Assert
        verify(userRepository, times(2)).saveAll(any());
    }
}
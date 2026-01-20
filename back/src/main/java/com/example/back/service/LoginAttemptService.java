package com.example.back.service;

import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_TIME_MINUTES = 15;

    public void loginFailed(String usernameOrEmail) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail).orElse(null);
        if (user != null && !user.getLockedByAdmin()) {
            int failedAttempts = user.getFailedLoginAttempts() != null ?
                    user.getFailedLoginAttempts() : 0;

            // Increment failed attempts
            failedAttempts++;
            user.setFailedLoginAttempts(failedAttempts);

            // Check if this is the 3rd failed attempt (MAX_FAILED_ATTEMPTS = 3)
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // Lock account for 15 minutes
                LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES);
                user.setAccountLockedUntil(lockUntil);
                // DON'T reset failed attempts here - keep them at 3
                // user.setFailedLoginAttempts(0); // REMOVE THIS LINE

                // Send email notification
                try {
                    emailService.sendAccountTemporarilyLockedEmail(
                            user.getEmail(),
                            user.getUsername(),
                            LOCK_TIME_MINUTES
                    );
                } catch (Exception e) {
                    // Log error but don't throw
                    e.printStackTrace();
                }
            }

            userRepository.save(user);
        }
    }

    public void loginSuccess(String usernameOrEmail) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail).orElse(null);
        if (user != null && !user.getLockedByAdmin()) {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        }
    }

    public int getRemainingAttempts(String usernameOrEmail) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail).orElse(null);
        if (user != null && !user.getLockedByAdmin()) {
            // Check if account is temporarily locked
            if (user.getAccountLockedUntil() != null &&
                    user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                return 0; // Account is locked, no attempts remaining
            }

            int failed = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
            return Math.max(0, MAX_FAILED_ATTEMPTS - failed);
        }
        return MAX_FAILED_ATTEMPTS;
    }
}
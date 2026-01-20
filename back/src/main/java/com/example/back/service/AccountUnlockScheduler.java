package com.example.back.service;  // Or whatever your service package is

import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j  // For logging
public class AccountUnlockScheduler {

    @Autowired
    private UserRepository userRepository;

    @Scheduled(cron = "0 */5 * * * *")  // Every 5 minutes (more efficient)
    @Transactional
    public void unlockTemporaryLocks() {
        try {
            // More efficient query using JPA
            List<User> lockedUsers = userRepository.findByAccountLockedUntilBeforeAndLockedByAdminFalse(
                    LocalDateTime.now()
            );

            for (User user : lockedUsers) {
                user.setAccountLockedUntil(null);
                user.setFailedLoginAttempts(0);
            }

            userRepository.saveAll(lockedUsers);

            if (!lockedUsers.isEmpty()) {
                log.info("Auto-unlocked {} user accounts", lockedUsers.size());
            }
        } catch (Exception e) {
            log.error("Error in unlockTemporaryLocks scheduler", e);
        }
    }
}
package com.example.back.service;

import com.example.back.entity.PasswordResetToken;
import com.example.back.entity.User;
import com.example.back.repository.PasswordResetTokenRepository;
import com.example.back.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PasswordService {

    private final UserRepository userRepository;

    private final PasswordResetTokenRepository tokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    public PasswordService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public boolean generateAndSendPasswordResetToken(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            PasswordResetToken resetToken;
            Optional<PasswordResetToken> existingTokenOptional = tokenRepository.findByUser(user);

            if (existingTokenOptional.isPresent()) {
                // Update existing token
                resetToken = existingTokenOptional.get();
                resetToken.setToken(resetToken.generateToken());
                resetToken.setExpiryDate(resetToken.calculateExpiryDate());
                resetToken.setUsed(false);
                tokenRepository.save(resetToken);
            } else {
                // Create new token
                resetToken = new PasswordResetToken(user);
                tokenRepository.save(resetToken);
            }

            try {
                // Send password reset email
                emailService.sendPasswordResetEmail(
                        user.getEmail(),
                        resetToken.getToken(),
                        user.getUsername()
                );
                return true;
            } catch (Exception e) {
                // If email fails, delete the token
                if (existingTokenOptional.isEmpty()) {
                    // Only delete if it was a newly created token
                    tokenRepository.delete(resetToken);
                }
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    // Validate reset token
    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        return tokenOptional.isPresent() &&
                !tokenOptional.get().isExpired() &&
                !tokenOptional.get().getUsed();
    }

    // Reset password with token
    @Transactional
    public boolean resetPasswordWithToken(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);

        if (tokenOptional.isPresent()) {
            PasswordResetToken resetToken = tokenOptional.get();

            if (resetToken.isExpired() || resetToken.getUsed()) {
                return false;
            }

            User user = resetToken.getUser();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            resetToken.setUsed(true);
            tokenRepository.save(resetToken);

            try {
                // Send confirmation email
                emailService.sendPasswordResetConfirmation(user.getEmail(), user.getUsername());
            } catch (Exception e) {
                // Log error but don't fail the reset operation
                e.printStackTrace();
            }

            return true;
        }

        return false;
    }

    // Change password for authenticated user
    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return false;
            }

            // Update to new password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return true;
        }

        return false;
    }

    // Check if email exists
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }


}
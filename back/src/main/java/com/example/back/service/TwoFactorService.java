package com.example.back.service;

import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Service
public class TwoFactorService {

    @Autowired
    private UserRepository userRepository;

    private final GoogleAuthenticator googleAuthenticator;
    private final SecureRandom secureRandom;

    public TwoFactorService() {
        this.googleAuthenticator = new GoogleAuthenticator();
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generate a 2FA secret for a user
     */
    public TwoFactorSetup generateSecret(User user) {
        // Generate secret key - retourne un GoogleAuthenticatorKey
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();

        // Récupérer la clé secrète sous forme de String pour stockage
        String secret = key.getKey();

        // Generate backup codes (5 codes of 8 characters each)
        List<String> backupCodes = generateBackupCodes(5);
        String backupCodesJson = String.join(",", backupCodes);

        // Save to user
        user.setTwoFactorSecret(secret);
        user.setTwoFactorBackupCodes(backupCodesJson);
        userRepository.save(user);

        // Generate QR code URL - passer l'objet GoogleAuthenticatorKey au lieu de la String
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                "ConventIA",
                user.getEmail(),
                key  // Passer l'objet key au lieu de la String secret
        );

        return new TwoFactorSetup(secret, qrCodeUrl, backupCodes);
    }

    /**
     * Generate backup codes
     */
    private List<String> generateBackupCodes(int count) {
        List<String> codes = new ArrayList<>();
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                code.append(characters.charAt(secureRandom.nextInt(characters.length())));
            }
            codes.add(code.toString());
        }

        return codes;
    }

    /**
     * Verify TOTP code
     */
    public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    /**
     * Verify a backup code
     */
    public boolean verifyBackupCode(User user, String backupCode) {
        String backupCodesJson = user.getTwoFactorBackupCodes();
        if (backupCodesJson == null || backupCodesJson.isEmpty()) {
            return false;
        }

        List<String> backupCodes = new ArrayList<>(Arrays.asList(backupCodesJson.split(",")));

        if (backupCodes.contains(backupCode)) {
            // Remove the used backup code
            backupCodes.remove(backupCode);
            user.setTwoFactorBackupCodes(String.join(",", backupCodes));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    /**
     * Enable 2FA for user
     */
    public void enableTwoFactor(User user) {
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    /**
     * Disable 2FA for user
     */
    public void disableTwoFactor(User user) {
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorBackupCodes(null);
        userRepository.save(user);
    }

    /**
     * Check if user has 2FA enabled
     */
    public boolean isTwoFactorEnabled(User user) {
        return user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled();
    }

    /**
     * DTO for 2FA setup response
     */
    public static class TwoFactorSetup {
        private final String secret;
        private final String qrCodeUrl;
        private final List<String> backupCodes;

        public TwoFactorSetup(String secret, String qrCodeUrl, List<String> backupCodes) {
            this.secret = secret;
            this.qrCodeUrl = qrCodeUrl;
            this.backupCodes = backupCodes;
        }

        public String getSecret() { return secret; }
        public String getQrCodeUrl() { return qrCodeUrl; }
        public List<String> getBackupCodes() { return backupCodes; }
    }
}
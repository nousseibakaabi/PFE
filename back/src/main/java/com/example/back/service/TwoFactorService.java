package com.example.back.service;

import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

@Service
public class TwoFactorService {

    private final UserRepository userRepository;

    private final GoogleAuthenticator googleAuthenticator;
    private final SecureRandom secureRandom;

    public TwoFactorService(UserRepository userRepository) {
        this.googleAuthenticator = new GoogleAuthenticator();
        this.secureRandom = new SecureRandom();
        this.userRepository = userRepository;
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


    public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }



    public void enableTwoFactor(User user) {
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }


    public void disableTwoFactor(User user) {
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorBackupCodes(null);
        userRepository.save(user);
    }


    public boolean isTwoFactorEnabled(User user) {
        return user.getTwoFactorEnabled() != null && user.getTwoFactorEnabled();
    }

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
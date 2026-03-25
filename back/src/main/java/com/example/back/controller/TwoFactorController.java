package com.example.back.controller;

import com.example.back.entity.User;
import com.example.back.payload.request.TwoFactorRequest;
import com.example.back.payload.response.MessageResponse;
import com.example.back.repository.UserRepository;
import com.example.back.security.services.UserDetailsImpl;
import com.example.back.service.TwoFactorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/2fa")
public class TwoFactorController {

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Setup 2FA - generates secret and QR code
     */
    @PostMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> setupTwoFactor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        TwoFactorService.TwoFactorSetup setup = twoFactorService.generateSecret(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("secret", setup.getSecret());
        response.put("qrCodeUrl", setup.getQrCodeUrl());
        response.put("backupCodes", setup.getBackupCodes());

        return ResponseEntity.ok(response);
    }

    /**
     * Verify and enable 2FA
     */
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verifyAndEnable(@Valid @RequestBody TwoFactorRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String secret = user.getTwoFactorSecret();
        if (secret == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("2FA not initialized. Please setup first."));
        }

        int code;
        try {
            code = Integer.parseInt(request.getCode());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid code format"));
        }

        if (twoFactorService.verifyCode(secret, code)) {
            twoFactorService.enableTwoFactor(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "2FA activée avec succès !");
            response.put("backupCodes", user.getTwoFactorBackupCodes().split(","));

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Code de vérification invalide"));
        }
    }

    /**
     * Disable 2FA
     */
    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> disableTwoFactor(@Valid @RequestBody TwoFactorRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify code before disabling
        String secret = user.getTwoFactorSecret();
        if (secret == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("2FA not enabled"));
        }

        int code;
        try {
            code = Integer.parseInt(request.getCode());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid code format"));
        }

        if (twoFactorService.verifyCode(secret, code)) {
            twoFactorService.disableTwoFactor(user);
            return ResponseEntity.ok(new MessageResponse("2FA désactivée avec succès !"));
        } else {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Code de vérification invalide"));
        }
    }

    /**
     * Get 2FA status
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTwoFactorStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("enabled", twoFactorService.isTwoFactorEnabled(user));

        return ResponseEntity.ok(response);
    }
}
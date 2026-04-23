package com.example.back.controller;

import com.example.back.payload.request.ChangePasswordRequest;
import com.example.back.payload.request.ForgotPasswordRequest;
import com.example.back.payload.request.ResetPasswordRequest;
import com.example.back.security.services.UserDetailsImpl;
import com.example.back.service.PasswordService;
import com.example.back.service.TokenService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class PasswordController {

    private final PasswordService passwordService;

    private final TokenService tokenService;

    public PasswordController(PasswordService passwordService, TokenService tokenService) {
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail();

        if (!passwordService.emailExists(email)) {
            return ResponseEntity.badRequest()
                    .body(tokenService.generateErrorResponse("No account found with this email address."));
        }

        // Generate token and send email
        boolean emailSent = passwordService.generateAndSendPasswordResetToken(email);

        if (emailSent) {
            Map<String, Object> response = tokenService.generateSuccessResponse(
                    "Password reset instructions have been sent to your email."
            );
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest()
                    .body(tokenService.generateErrorResponse("Failed to send password reset email. Please try again."));
        }
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        boolean isValid = passwordService.validateResetToken(token);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("token", token);
        response.put("valid", isValid);
        response.put("message", isValid ? "Token is valid" : "Token is invalid or expired");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // Check if passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(tokenService.generateErrorResponse("Passwords do not match"));
        }

        boolean success = passwordService.resetPasswordWithToken(
                request.getToken(),
                request.getNewPassword()
        );

        if (success) {
            return ResponseEntity.ok(tokenService.generateSuccessResponse(
                    "Password reset successfully! You can now login with your new password."
            ));
        } else {
            return ResponseEntity.badRequest()
                    .body(tokenService.generateErrorResponse("Invalid or expired token"));
        }
    }

    // ========== CHANGE PASSWORD (Authenticated) ==========

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Check if new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(tokenService.generateErrorResponse("New password and confirmation do not match"));
        }

        // Change password
        boolean success = passwordService.changePassword(
                userDetails.getId(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );

        if (success) {
            return ResponseEntity.ok(tokenService.generateSuccessResponse(
                    "Password changed successfully!"
            ));
        } else {
            return ResponseEntity.badRequest()
                    .body(tokenService.generateErrorResponse("Current password is incorrect"));
        }
    }

    // ========== TEST ENDPOINTS ==========

    @PostMapping("/quick-reset")
    public ResponseEntity<?> quickReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");

        if (email == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(tokenService.generateErrorResponse("Email and newPassword are required"));
        }

        return ResponseEntity.ok(tokenService.generateSuccessResponse(
                "If an account exists with that email, password reset instructions have been sent."
        ));
    }
}
package com.example.back.controller;


import com.example.back.entity.User;
import com.example.back.payload.request.LoginRequest;
import com.example.back.payload.response.JwtResponse;
import com.example.back.payload.response.MessageResponse;
import com.example.back.repository.UserRepository;
import com.example.back.security.jwt.JwtUtils;
import com.example.back.security.services.UserDetailsImpl;
import com.example.back.service.EmailService;
import com.example.back.service.HistoryService;
import com.example.back.service.LoginAttemptService;
import com.example.back.service.TwoFactorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private HistoryService historyService;


    @Autowired
    private TwoFactorService twoFactorService;


    private static final int MAX_FAILED_ATTEMPTS = 3;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        try {
            // Vérifier si l'utilisateur existe
            User existingUser = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()).orElse(null);

            if (existingUser != null) {

                System.out.println("=== LOGIN DEBUG ===");
                System.out.println("User: " + existingUser.getUsername());
                System.out.println("Enabled: " + existingUser.getEnabled());
                System.out.println("LockedByAdmin: " + existingUser.getLockedByAdmin());
                System.out.println("AccountNonLocked: " + existingUser.getAccountNonLocked());
                System.out.println("AccountLockedUntil: " + existingUser.getAccountLockedUntil());
                System.out.println("TwoFactorEnabled: " + existingUser.getTwoFactorEnabled());
                System.out.println("==================");

                // Check if locked by admin
                if (existingUser.getLockedByAdmin()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Account locked by administrator. Contact admin to unlock.");
                    response.put("error", "AccountLocked");
                    response.put("lockType", "ADMIN");
                    return ResponseEntity.status(401).body(response);
                }

                // Check if temporarily locked
                if (existingUser.getAccountLockedUntil() != null &&
                        existingUser.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Account is temporarily locked due to too many failed attempts");
                    response.put("error", "AccountTemporarilyLocked");
                    response.put("remainingAttempts", 0);
                    response.put("lockType", "TEMPORARY");
                    response.put("lockUntil", existingUser.getAccountLockedUntil());
                    return ResponseEntity.status(401).body(response);
                }
            }

            // Get remaining attempts before authentication
            int remainingAttempts = loginAttemptService.getRemainingAttempts(loginRequest.getUsernameOrEmail());

            if (remainingAttempts <= 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Account is temporarily locked due to too many failed attempts");
                response.put("error", "AccountTemporarilyLocked");
                response.put("remainingAttempts", 0);
                return ResponseEntity.status(401).body(response);
            }

            // Vérifier si la 2FA est activée (avant l'authentification pour éviter de valider le mot de passe deux fois)
            boolean isTwoFactorEnabled = existingUser != null && twoFactorService.isTwoFactorEnabled(existingUser);

            // Si la 2FA est activée, on vérifie d'abord le mot de passe
            if (isTwoFactorEnabled) {
                try {
                    // Authentifier d'abord pour vérifier le mot de passe
                    Authentication authentication = authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(),
                                    loginRequest.getPassword()));

                    // Si l'authentification réussit mais que la 2FA est activée
                    // Générer un token temporaire pour la session 2FA
                    String tempToken = UUID.randomUUID().toString();

                    // Stocker le token temporaire avec l'ID utilisateur
                    sessionCache.put(tempToken, existingUser.getId());

                    // Réinitialiser les tentatives de login car le mot de passe est correct
                    loginAttemptService.loginSuccess(loginRequest.getUsernameOrEmail());

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("requiresTwoFactor", true);
                    response.put("tempToken", tempToken);
                    response.put("message", "2FA verification required");

                    return ResponseEntity.ok(response);

                } catch (BadCredentialsException e) {
                    // Mot de passe incorrect - gérer comme une erreur normale
                    return handleBadCredentials(loginRequest);
                }
            }

            // Si la 2FA n'est pas activée, procéder normalement
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()));

            // Login successful - reset attempts
            loginAttemptService.loginSuccess(loginRequest.getUsernameOrEmail());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Update last login time
            User user = userRepository.findById(userDetails.getId()).orElseThrow();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // LOG HISTORY: User login
            historyService.logUserLogin(user);

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getFirstName(),
                    userDetails.getLastName(),
                    roles));

        } catch (BadCredentialsException e) {
            return handleBadCredentials(loginRequest);

        } catch (DisabledException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Account is disabled");
            response.put("error", "AccountDisabled");
            return ResponseEntity.status(401).body(response);

        } catch (LockedException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Account is locked");
            response.put("error", "AccountLocked");

            // Check if locked by admin or by failed attempts
            User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()).orElse(null);
            if (user != null) {
                if (user.getLockedByAdmin()) {
                    response.put("lockType", "ADMIN");
                    response.put("message", "Account locked by administrator. Contact admin to unlock.");

                    // Send email for admin lock
                    try {
                        emailService.sendAccountLockedByAdminEmail(user.getEmail(), user.getUsername());
                    } catch (Exception emailEx) {
                        response.put("Failed to send admin lock email: {}", emailEx.getMessage());
                    }
                } else if (user.getAccountLockedUntil() != null) {
                    response.put("lockType", "TEMPORARY");
                    response.put("lockUntil", user.getAccountLockedUntil());

                    // Calculate remaining time
                    long minutesRemaining = java.time.Duration.between(
                            LocalDateTime.now(),
                            user.getAccountLockedUntil()
                    ).toMinutes();

                    response.put("message", "Account temporarily locked. Try again in " + minutesRemaining + " minutes.");

                    // Send email for temporary lock
                    try {
                        emailService.sendAccountTemporarilyLockedEmail(
                                user.getEmail(),
                                user.getUsername(),
                                minutesRemaining
                        );
                    } catch (Exception emailEx) {
                        response.put("Failed to send temporary lock email: {}", emailEx.getMessage());
                    }
                }
            }

            return ResponseEntity.status(401).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Authentication failed");
            response.put("error", e.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }

    // Méthode helper pour gérer les erreurs de mot de passe
    private ResponseEntity<?> handleBadCredentials(LoginRequest loginRequest) {
        // Login failed - increment attempts
        loginAttemptService.loginFailed(loginRequest.getUsernameOrEmail());

        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()).orElse(null);
        int failedAttempts = user != null ? (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) : 0;
        int remainingAttempts = MAX_FAILED_ATTEMPTS - failedAttempts;

        Map<String, Object> response = new HashMap<>();

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            // Account should be locked now
            response.put("success", false);
            response.put("message", "Account locked for 15 minutes due to too many failed attempts");
            response.put("error", "AccountTemporarilyLocked");
            response.put("remainingAttempts", 0);
            response.put("lockType", "TEMPORARY");
            if (user != null && user.getAccountLockedUntil() != null) {
                response.put("lockUntil", user.getAccountLockedUntil());
            }
        } else if (failedAttempts == MAX_FAILED_ATTEMPTS - 1) {
            // This is the last attempt before lock (2nd failed attempt)
            response.put("success", false);
            response.put("message", "One more failed attempt will lock your account for 15 minutes");
            response.put("error", "LastAttemptWarning");
            response.put("remainingAttempts", 1);
            response.put("isLastAttempt", true);
        } else {
            // Normal failed attempt
            response.put("success", false);
            response.put("message", "Invalid username/email or password");
            response.put("error", "BadCredentials");
            response.put("remainingAttempts", remainingAttempts);

            // Add specific messages
            if (remainingAttempts == 2) {
                response.put("userMessage", "Invalid credentials. 2 attempts remaining");
            } else if (remainingAttempts == 1) {
                response.put("userMessage", "Invalid credentials. 1 attempt remaining");
            }
        }

        return ResponseEntity.status(401).body(response);
    }


    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verifyTwoFactor(@RequestBody Map<String, String> request) {
        String tempToken = request.get("tempToken");
        String code = request.get("code");

        // Récupérer l'ID utilisateur depuis le cache temporaire
        Long userId = sessionCache.get(tempToken);
        if (userId == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid or expired session"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("User not found"));
        }

        // Vérifier le code 2FA
        int verificationCode;
        try {
            verificationCode = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid code format"));
        }

        if (twoFactorService.verifyCode(user.getTwoFactorSecret(), verificationCode)) {
            // Code valide - générer le JWT final
            UserDetailsImpl userDetails = UserDetailsImpl.build(user);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtUtils.generateJwtToken(authentication);

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Update last login time
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // LOG HISTORY
            historyService.logUserLogin(user);

            // Supprimer le token temporaire
            sessionCache.remove(tempToken);

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getFirstName(),
                    userDetails.getLastName(),
                    roles));
        } else {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Invalid 2FA code"));
        }
    }

    // Cache simple pour stocker les tokens temporaires (à remplacer par Redis en production)
    private final Map<String, Long> sessionCache = new ConcurrentHashMap<>();


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        // Get current user for history
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            userRepository.findByUsername(auth.getName()).ifPresent(user -> {
                // LOG HISTORY: User logout
                historyService.logUserLogout(user);
            });
        }

        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }
}
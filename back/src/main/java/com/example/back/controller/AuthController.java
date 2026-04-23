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
import jakarta.validation.Valid;
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

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final JwtUtils jwtUtils;

    private final LoginAttemptService loginAttemptService;

    private final EmailService emailService;

    private final HistoryService historyService;

    private final TwoFactorService twoFactorService;

    private static final int MAX_FAILED_ATTEMPTS = 3;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository, JwtUtils jwtUtils, LoginAttemptService loginAttemptService, EmailService emailService, HistoryService historyService, TwoFactorService twoFactorService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
        this.historyService = historyService;
        this.twoFactorService = twoFactorService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        try {
            // Vérifier si l'utilisateur existe
            User existingUser = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()).orElse(null);

            if (existingUser != null) {

                System.out.println("=== DEBUG CONNEXION ===");
                System.out.println("Utilisateur: " + existingUser.getUsername());
                System.out.println("Activé: " + existingUser.getEnabled());
                System.out.println("Verrouillé par admin: " + existingUser.getLockedByAdmin());
                System.out.println("Compte non verrouillé: " + existingUser.getAccountNonLocked());
                System.out.println("Compte verrouillé jusqu'au: " + existingUser.getAccountLockedUntil());
                System.out.println("2FA activée: " + existingUser.getTwoFactorEnabled());
                System.out.println("=======================");

                // Vérifier si verrouillé par l'administrateur
                if (existingUser.getLockedByAdmin()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Compte verrouillé par l'administrateur. Contactez l'admin pour déverrouiller.");
                    response.put("error", "AccountLocked");
                    response.put("lockType", "ADMIN");
                    return ResponseEntity.status(401).body(response);
                }

// Vérifier si temporairement verrouillé
                if (existingUser.getAccountLockedUntil() != null &&
                        existingUser.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Le compte est temporairement verrouillé en raison de trop de tentatives échouées");
                    response.put("error", "AccountTemporarilyLocked");
                    response.put("remainingAttempts", 0);
                    response.put("lockType", "TEMPORARY");
                    response.put("lockUntil", existingUser.getAccountLockedUntil().toString());

                    // Calculate remaining time
                    long minutesRemaining = java.time.Duration.between(
                            LocalDateTime.now(),
                            existingUser.getAccountLockedUntil()
                    ).toMinutes();
                    response.put("minutesRemaining", minutesRemaining);

                    return ResponseEntity.status(401).body(response);
                }

            }

            // Obtenir les tentatives restantes avant l'authentification
            int remainingAttempts = loginAttemptService.getRemainingAttempts(loginRequest.getUsernameOrEmail());

            if (remainingAttempts <= 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Le compte est temporairement verrouillé en raison de trop de tentatives échouées");
                response.put("error", "AccountTemporarilyLocked");
                response.put("remainingAttempts", 0);
                return ResponseEntity.status(401).body(response);
            }

            // Vérifier si la 2FA est activée (avant l'authentification pour éviter de valider le mot de passe deux fois)
            boolean isTwoFactorEnabled = existingUser != null && twoFactorService.isTwoFactorEnabled(existingUser);

            // Si la 2FA est activée, on vérifie d'abord le mot de passe
            if (isTwoFactorEnabled) {
                try {

                    String tempToken = UUID.randomUUID().toString();

                    sessionCache.put(tempToken, existingUser.getId());

                    loginAttemptService.loginSuccess(loginRequest.getUsernameOrEmail());

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("requiresTwoFactor", true);
                    response.put("tempToken", tempToken);
                    response.put("message", "Vérification 2FA requise");

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

            // Connexion réussie - réinitialiser les tentatives
            loginAttemptService.loginSuccess(loginRequest.getUsernameOrEmail());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Mettre à jour la dernière connexion
            User user = userRepository.findById(userDetails.getId()).orElseThrow();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // HISTORIQUE: Connexion utilisateur
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
            response.put("message", "Le compte est désactivé");
            response.put("error", "AccountDisabled");
            return ResponseEntity.status(401).body(response);

        }catch (LockedException e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Le compte est verrouillé");
                response.put("error", "AccountLocked");

                User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()).orElse(null);
                if (user != null) {
                    if (user.getLockedByAdmin()) {
                        response.put("lockType", "ADMIN");
                        response.put("message", "Compte verrouillé par l'administrateur. Contactez l'admin pour déverrouiller.");

                        try {
                            System.out.println("Tentative d'envoi d'email de verrouillage admin à: " + user.getEmail());
                            emailService.sendAccountLockedByAdminEmail(user.getEmail(), user.getUsername());
                            System.out.println("Email de verrouillage admin envoyé avec succès");
                        } catch (Exception emailEx) {
                            System.err.println("Échec d'envoi de l'email de verrouillage admin: " + emailEx.getMessage());
                            emailEx.printStackTrace();
                            response.put("emailError", "Échec d'envoi de l'email de verrouillage admin: " + emailEx.getMessage());
                        }
                    } else if (user.getAccountLockedUntil() != null &&
                            user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                        response.put("lockType", "TEMPORARY");
                        response.put("lockUntil", user.getAccountLockedUntil().toString());

                        long minutesRemaining = java.time.Duration.between(
                                LocalDateTime.now(),
                                user.getAccountLockedUntil()
                        ).toMinutes();

                        response.put("minutesRemaining", minutesRemaining);
                        response.put("message", "Compte temporairement verrouillé. Réessayez dans " + minutesRemaining + " minutes.");

                        try {
                            emailService.sendAccountTemporarilyLockedEmail(
                                    user.getEmail(),
                                    user.getUsername(),
                                    minutesRemaining
                            );
                        } catch (Exception emailEx) {
                            System.err.println("Échec d'envoi de l'email de verrouillage temporaire: " + emailEx.getMessage());
                            emailEx.printStackTrace();
                        }
                    }
                }

                return ResponseEntity.status(401).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Échec de l'authentification");
            response.put("error", e.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }


    // Méthode helper pour gérer les erreurs de mot de passe
    private ResponseEntity<?> handleBadCredentials(LoginRequest loginRequest) {
        // Échec de connexion - incrémenter les tentatives
        loginAttemptService.loginFailed(loginRequest.getUsernameOrEmail());

        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()).orElse(null);

        // Get fresh user data after loginFailed (to get updated lockUntil)
        if (user != null) {
            user = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()).orElse(null);
        }

        int failedAttempts = user != null ? (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) : 0;
        int remainingAttempts = MAX_FAILED_ATTEMPTS - failedAttempts;

        Map<String, Object> response = new HashMap<>();

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            // Account is locked
            response.put("success", false);
            response.put("message", "Compte verrouillé pour 15 minutes en raison de trop de tentatives échouées");
            response.put("error", "AccountTemporarilyLocked");
            response.put("remainingAttempts", 0);
            response.put("lockType", "TEMPORARY");

            if (user != null && user.getAccountLockedUntil() != null) {
                // Send lockUntil as string
                response.put("lockUntil", user.getAccountLockedUntil().toString());

                // Calculate remaining time
                long minutesRemaining = java.time.Duration.between(
                        LocalDateTime.now(),
                        user.getAccountLockedUntil()
                ).toMinutes();

                long secondsRemaining = java.time.Duration.between(
                        LocalDateTime.now(),
                        user.getAccountLockedUntil()
                ).getSeconds();

                response.put("minutesRemaining", minutesRemaining);
                response.put("secondsRemaining", secondsRemaining);

                System.out.println("=== ACCOUNT LOCKED ===");
                System.out.println("User: " + user.getUsername());
                System.out.println("Locked until: " + user.getAccountLockedUntil());
                System.out.println("Minutes remaining: " + minutesRemaining);
                System.out.println("Seconds remaining: " + secondsRemaining);
                System.out.println("======================");
            }
        } else if (failedAttempts == MAX_FAILED_ATTEMPTS - 1) {
            // Last attempt before lock (2nd failed attempt)
            response.put("success", false);
            response.put("message", "⚠️ ATTENTION : Une tentative supplémentaire échouée verrouillera votre compte pour 15 minutes !");
            response.put("error", "LastAttemptWarning");
            response.put("remainingAttempts", 1);
            response.put("isLastAttempt", true);
        } else {
            // Normal failed attempt
            response.put("success", false);
            response.put("message", "❌ Nom d'utilisateur/email ou mot de passe invalide");
            response.put("error", "BadCredentials");
            response.put("remainingAttempts", remainingAttempts);

            // Add specific messages based on remaining attempts
            if (remainingAttempts == 2) {
                response.put("userMessage", "❌ Identifiants invalides. 2 tentatives restantes");
            } else if (remainingAttempts == 1) {
                response.put("userMessage", "❌ Identifiants invalides. 1 tentative restante");
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
                    .body(new MessageResponse("Session invalide ou expirée"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Utilisateur non trouvé"));
        }

        // Vérifier le code 2FA
        int verificationCode;
        try {
            verificationCode = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Format de code invalide"));
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

            // Mettre à jour la dernière connexion
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // HISTORIQUE
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
                    .body(new MessageResponse("Code 2FA invalide"));
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
        response.put("message", "Échec de la validation");
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // Récupérer l'utilisateur actuel pour l'historique
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            userRepository.findByUsername(auth.getName()).ifPresent(historyService::logUserLogout);
        }

        return ResponseEntity.ok(new MessageResponse("Déconnexion réussie"));
    }
}
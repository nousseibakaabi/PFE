package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.Role;
import com.example.back.entity.User;
import com.example.back.payload.request.LoginRequest;
import com.example.back.payload.request.SignupRequest;
import com.example.back.payload.response.JwtResponse;
import com.example.back.payload.response.MessageResponse;
import com.example.back.repository.RoleRepository;
import com.example.back.repository.UserRepository;
import com.example.back.security.jwt.JwtUtils;
import com.example.back.security.services.UserDetailsImpl;
import com.example.back.service.AvatarService;
import com.example.back.service.EmailService;
import com.example.back.service.LoginAttemptService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AvatarService avatarService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private EmailService emailService;

    private static final int MAX_FAILED_ATTEMPTS = 3;


    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Check if account is already locked
            User existingUser = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()).orElse(null);

            if (existingUser != null) {
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

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getFirstName(),
                    userDetails.getLastName(),
                    roles));

        } catch (BadCredentialsException e) {
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

        catch (DisabledException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Account is disabled");
            response.put("error", "AccountDisabled");
            return ResponseEntity.status(401).body(response);
        }

        catch (LockedException e) {
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

        }

        catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Authentication failed");
            response.put("error", e.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        String avatarUrl = avatarService.generateAvatarUrlForSignup(
                signUpRequest.getFirstName(),
                signUpRequest.getLastName(),
                signUpRequest.getUsername()
        );

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setPhone(signUpRequest.getPhone());
        user.setDepartment(signUpRequest.getDepartment());
        user.setProfileImage(avatarUrl);


        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_COMMERCIAL_METIER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role.toLowerCase()) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "commercial":
                        Role commercialRole = roleRepository.findByName(ERole.ROLE_COMMERCIAL_METIER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(commercialRole);
                        break;
                    case "decideur":
                        Role decideurRole = roleRepository.findByName(ERole.ROLE_DECIDEUR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(decideurRole);
                        break;
                    case "chef_projet":
                        Role chefProjetRole = roleRepository.findByName(ERole.ROLE_CHEF_PROJET)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(chefProjetRole);
                        break;
                    default:
                        Role defaultRole = roleRepository.findByName(ERole.ROLE_COMMERCIAL_METIER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(defaultRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }


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
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

}
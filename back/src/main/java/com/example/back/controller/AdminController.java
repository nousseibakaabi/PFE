package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.Role;
import com.example.back.entity.User;
import com.example.back.payload.request.SignupRequest;
import com.example.back.repository.RoleRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.AvatarService;
import com.example.back.service.EmailService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AvatarService avatarService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> allUsers = userRepository.findAll();

        // Filter out users with ADMIN role
        List<User> nonAdminUsers = allUsers.stream()
                .filter(user -> user.getRoles().stream()
                        .noneMatch(role -> role.getName() == ERole.ROLE_ADMIN))  
                .collect(Collectors.toList());

        return ResponseEntity.ok(nonAdminUsers);
    }

    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getLockedByAdmin()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User is already locked by admin");
                return ResponseEntity.badRequest().body(response);
            }

            user.setLockedByAdmin(true);
            user.setAccountLockedUntil(null);
            userRepository.save(user);

            try {
                emailService.sendAccountLockedByAdminEmail(
                        user.getEmail(),
                        user.getUsername()
                );
                log.info("Admin lock email sent to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send admin lock email: {}", e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User locked successfully. Notification email sent.");
            response.put("userId", userId);
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to lock user: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.getLockedByAdmin()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User is not locked by admin");
                return ResponseEntity.badRequest().body(response);
            }

            user.setLockedByAdmin(false);
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            try {
                emailService.sendAccountUnlockedByAdminEmail(
                        user.getEmail(),
                        user.getUsername()
                );
                log.info("Admin unlock email sent to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send admin unlock email: {}", e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User unlocked successfully. Notification email sent.");
            response.put("userId", userId);
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to unlock user: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/users/locked")
    public ResponseEntity<List<User>> getLockedUsers() {
        List<User> lockedUsers = userRepository.findAll().stream()
                .filter(user -> user.getLockedByAdmin() ||
                        (user.getAccountLockedUntil() != null &&
                                user.getAccountLockedUntil().isAfter(LocalDateTime.now())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(lockedUsers);
    }

    @PostMapping("/users/add")
    public ResponseEntity<?> addUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Username is already taken!");
                return ResponseEntity.badRequest().body(response);
            }

            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email is already in use!");
                return ResponseEntity.badRequest().body(response);
            }

            String avatarUrl = avatarService.generateAvatarUrlForSignup(
                    signUpRequest.getFirstName(),
                    signUpRequest.getLastName(),
                    signUpRequest.getUsername()
            );

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
                Role defaultRole = roleRepository.findByName(ERole.ROLE_COMMERCIAL_METIER)
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(defaultRole);
            } else {
                strRoles.forEach(role -> {
                    if (!role.equalsIgnoreCase("admin")) {
                        switch (role.toLowerCase()) {
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
                    }
                });
            }

            user.setRoles(roles);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User created successfully!");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create user: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/users/{userId}/department")
    public ResponseEntity<?> updateUserDepartment(@PathVariable Long userId,
                                                  @RequestBody Map<String, String> request) {
        try {
            String department = request.get("department");

            if (department == null || department.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Department is required");
                return ResponseEntity.badRequest().body(response);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setDepartment(department.trim());
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Department updated successfully");
            response.put("userId", userId);
            response.put("department", department);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update department: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long userId,
                                             @RequestBody Map<String, List<String>> request) {
        try {
            List<String> strRoles = request.get("roles");

            if (strRoles == null || strRoles.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "At least one role is required");
                return ResponseEntity.badRequest().body(response);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Set<Role> roles = new HashSet<>();

            for (String roleName : strRoles) {
                if (!roleName.equalsIgnoreCase("admin")) {
                    switch (roleName.toLowerCase()) {
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
                            break;
                    }
                }
            }

            if (roles.isEmpty()) {
                Role defaultRole = roleRepository.findByName(ERole.ROLE_COMMERCIAL_METIER)
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(defaultRole);
            }

            user.setRoles(roles);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Roles updated successfully");
            response.put("userId", userId);
            response.put("roles", strRoles);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update roles: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/roles/available")
    public ResponseEntity<?> getAvailableRoles() {
        try {
            List<Map<String, String>> roles = new ArrayList<>();

            Map<String, String> commercial = new HashMap<>();
            commercial.put("value", "commercial");
            commercial.put("label", "Commercial Métier");
            roles.add(commercial);

            Map<String, String> decideur = new HashMap<>();
            decideur.put("value", "decideur");
            decideur.put("label", "Décideur");
            roles.add(decideur);

            Map<String, String> chefProjet = new HashMap<>();
            chefProjet.put("value", "chef_projet");
            chefProjet.put("label", "Chef de Projet");
            roles.add(chefProjet);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("roles", roles);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get roles: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
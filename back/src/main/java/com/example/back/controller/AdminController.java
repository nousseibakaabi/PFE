package com.example.back.controller;

import com.example.back.entity.ERole;
import com.example.back.entity.Role;
import com.example.back.entity.User;
import com.example.back.payload.request.SignupRequest;
import com.example.back.payload.response.MessageResponse;
import com.example.back.repository.RoleRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.AvatarService;
import com.example.back.service.EmailService;
import com.example.back.service.HistoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired
    private HistoryService historyService;


    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> allUsers = userRepository.findAll();

        List<User> nonAdminUsers = allUsers.stream()
                .filter(user -> user.getRoles().stream()
                        .noneMatch(role -> role.getName() == ERole.ROLE_ADMIN))
                .filter(user -> !"system".equals(user.getUsername()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(nonAdminUsers);
    }


    @PostMapping("/users/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> lockUser(@PathVariable Long userId) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            targetUser.setLockedByAdmin(true);
            targetUser.setAccountNonLocked(false);
            User updatedUser = userRepository.save(targetUser);

            // LOG HISTORY: User lock
            historyService.logUserLock(targetUser, currentUser);

            return ResponseEntity.ok(new MessageResponse("User locked successfully"));
        } catch (Exception e) {
            log.error("Error locking user: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/users/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unlockUser(@PathVariable Long userId) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            targetUser.setLockedByAdmin(false);
            targetUser.setAccountNonLocked(true);
            targetUser.setFailedLoginAttempts(0);
            targetUser.setAccountLockedUntil(null);
            User updatedUser = userRepository.save(targetUser);

            // LOG HISTORY: User unlock
            historyService.logUserUnlock(targetUser, currentUser);

            return ResponseEntity.ok(new MessageResponse("User unlocked successfully"));
        } catch (Exception e) {
            log.error("Error unlocking user: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
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


            if (signUpRequest.getPhone() != null && !signUpRequest.getPhone().trim().isEmpty()) {
                if (userRepository.existsByPhone(signUpRequest.getPhone())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Phone number is already in use!");
                    return ResponseEntity.badRequest().body(response);
                }
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
            user.setNotifMode("email");


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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserDepartment(@PathVariable Long userId,
                                                  @RequestBody Map<String, String> request) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String oldDepartment = targetUser.getDepartment();
            String newDepartment = request.get("department");

            targetUser.setDepartment(newDepartment);
            User updatedUser = userRepository.save(targetUser);

            // LOG HISTORY: Department change
            historyService.logUserDepartmentChange(targetUser, currentUser, oldDepartment, newDepartment);

            return ResponseEntity.ok(new MessageResponse("User department updated successfully"));
        } catch (Exception e) {
            log.error("Error updating user department: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }



    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRoles(@PathVariable Long userId,
                                             @RequestBody Map<String, List<String>> request) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<String> oldRoles = targetUser.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            List<String> newRoleValues = request.get("roles");
            Set<Role> newRoles = new HashSet<>();

            for (String roleValue : newRoleValues) {
                Role role = roleRepository.findByName(ERole.valueOf(roleValue))
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleValue));
                newRoles.add(role);
            }

            targetUser.setRoles(newRoles);
            User updatedUser = userRepository.save(targetUser);

            // LOG HISTORY: Role change
            historyService.logUserRoleChange(targetUser, currentUser, oldRoles,
                    newRoleValues.stream().collect(Collectors.toList()));

            return ResponseEntity.ok(new MessageResponse("User roles updated successfully"));
        } catch (Exception e) {
            log.error("Error updating user roles: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Object createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }


    /**
     * Get current user
     */
    private User getCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(currentUsername).orElse(null);
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
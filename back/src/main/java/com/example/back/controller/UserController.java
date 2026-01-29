// UserController.java
package com.example.back.controller;

import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();

            List<Map<String, Object>> userResponses = users.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userResponses);
            response.put("count", users.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching users: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsersByRole(@PathVariable String roleName) {
        try {
            String roleWithPrefix = "ROLE_" + roleName.toUpperCase();

            List<User> users = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().stream()
                            .anyMatch(role -> role.getName().name().equals(roleWithPrefix)))
                    .collect(Collectors.toList());

            List<Map<String, Object>> userResponses = users.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userResponses);
            response.put("count", users.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching users by role: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> convertToResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("phone", user.getPhone());
        response.put("department", user.getDepartment());
        response.put("enabled", user.getEnabled());
        response.put("profileImage", user.getProfileImage());

        // Get role names
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        response.put("roles", roles);

        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
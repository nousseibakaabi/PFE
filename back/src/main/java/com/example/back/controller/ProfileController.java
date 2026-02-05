package com.example.back.controller;

import com.example.back.entity.User;
import com.example.back.payload.request.ProfileUpdateRequest;
import com.example.back.payload.response.MessageResponse;
import com.example.back.payload.response.ProfileResponse;
import com.example.back.repository.UserRepository;
import com.example.back.security.services.UserDetailsImpl;
import com.example.back.service.AvatarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AvatarService avatarService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        ProfileResponse profileResponse = new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getDepartment(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getLastLogin(),
                roles,
                user.getProfileImage()
        );

        return ResponseEntity.ok(profileResponse);
    }

    @PutMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user fields
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }
        if (updateRequest.getDepartment() != null) {
            user.setDepartment(updateRequest.getDepartment());
        }
        // Profile image is now handled separately via upload

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Profile updated successfully!"));
    }

    @PostMapping("/update-with-avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfileWithAvatar(
            @RequestParam(value = "avatar", required = false) MultipartFile avatarFile,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("phone") String phone,
            @RequestParam("department") String department) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user fields
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setDepartment(department);

        // Handle avatar upload if provided
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String avatarUrl = saveAvatarFile(avatarFile, user.getUsername());
                user.setProfileImage(avatarUrl);
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(new MessageResponse("Failed to upload avatar image"));
            }
        }

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Profile updated successfully!"));
    }

    private String saveAvatarFile(MultipartFile file, String username) throws IOException {
        String uploadDir = "uploads/avatars";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = username + "_" + UUID.randomUUID().toString() + fileExtension;

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

        return "/uploads/avatars/" + newFilename;
    }

    @GetMapping("/debug-avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> debugAvatar() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("userId", user.getId());
        debugInfo.put("username", user.getUsername());
        debugInfo.put("profileImage", user.getProfileImage());
        debugInfo.put("profileImageExists", user.getProfileImage() != null);

        if (user.getProfileImage() != null) {
            // Check if it's a file path or base64
            if (user.getProfileImage().startsWith("/uploads/")) {
                String filename = user.getProfileImage().replace("/uploads/avatars/", "");
                Path filePath = Paths.get("uploads", "avatars", filename);
                debugInfo.put("isFilePath", true);
                debugInfo.put("absolutePath", filePath.toAbsolutePath().toString());
                debugInfo.put("fileExists", Files.exists(filePath));

                if (Files.exists(filePath)) {
                    try {
                        debugInfo.put("fileSize", Files.size(filePath));
                        debugInfo.put("fileUrl", "http://localhost:8084" + user.getProfileImage());
                    } catch (IOException e) {
                        debugInfo.put("fileSizeError", e.getMessage());
                    }
                }
            } else if (user.getProfileImage().startsWith("data:image")) {
                debugInfo.put("isBase64", true);
                debugInfo.put("dataLength", user.getProfileImage().length());
            }
        }

        return ResponseEntity.ok(debugInfo);
    }


    // Add this method to ProfileController.java
    @PutMapping("/update-notification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateNotificationMode(@RequestBody Map<String, String> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String notifMode = request.get("notifMode");

        if (notifMode == null || !(notifMode.equals("email") || notifMode.equals("sms") || notifMode.equals("both"))) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid notification mode. Use 'email', 'sms', or 'both'."));
        }

        user.setNotifMode(notifMode);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Notification preferences updated successfully!"));
    }
}
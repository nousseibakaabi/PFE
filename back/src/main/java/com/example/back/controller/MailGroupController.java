package com.example.back.controller;

import com.example.back.entity.User;
import com.example.back.payload.request.MailGroupRequest;
import com.example.back.payload.response.EmailSuggestionResponse;
import com.example.back.payload.response.MailGroupResponse;
import com.example.back.repository.UserRepository;
import com.example.back.service.MailGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mails/groups")
@Slf4j
public class MailGroupController {

    private final MailGroupService groupService;

    private final UserRepository userRepository;

    public MailGroupController(MailGroupService groupService, UserRepository userRepository) {
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    // GET endpoints - accessible to all authenticated users
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserGroups() {
        try {
            User currentUser = getCurrentUser();
            List<MailGroupResponse> groups = groupService.getUserGroups(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", groups);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching groups: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch groups"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getGroup(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            MailGroupResponse group = groupService.getGroup(id, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", group);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching group: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/suggestions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSuggestions(@RequestParam String q) {
        try {
            User currentUser = getCurrentUser();
            List<EmailSuggestionResponse> suggestions = groupService.searchEmailsAndGroups(q, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", suggestions);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting suggestions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to get suggestions"));
        }
    }

    // CREATE - all authenticated users can create groups
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createGroup(@Valid @RequestBody MailGroupRequest request) {
        try {
            User currentUser = getCurrentUser();
            MailGroupResponse group = groupService.createGroup(request, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group created successfully");
            response.put("data", group);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating group: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // UPDATE - only owner can update (checked in service)
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateGroup(@PathVariable Long id, @Valid @RequestBody MailGroupRequest request) {
        try {
            User currentUser = getCurrentUser();
            MailGroupResponse group = groupService.updateGroup(id, request, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group updated successfully");
            response.put("data", group);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating group: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // DELETE - only owner can delete (checked in service)
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteGroup(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            groupService.deleteGroup(id, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting group: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // MEMBER MANAGEMENT - only owner can modify members (checked in service)
    @PostMapping("/{groupId}/members/{memberId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        try {
            User currentUser = getCurrentUser();
            MailGroupResponse group = groupService.addMember(groupId, memberId, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member added successfully");
            response.put("data", group);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error adding member: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        try {
            User currentUser = getCurrentUser();
            MailGroupResponse group = groupService.removeMember(groupId, memberId, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Member removed successfully");
            response.put("data", group);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error removing member: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
}
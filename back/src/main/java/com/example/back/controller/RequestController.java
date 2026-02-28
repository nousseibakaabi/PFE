// RequestController.java
package com.example.back.controller;

import com.example.back.entity.User;
import com.example.back.payload.request.RequestActionDTO;
import com.example.back.payload.response.RequestResponse;
import com.example.back.repository.UserRepository;
import com.example.back.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@Slf4j
public class RequestController {

    @Autowired
    private RequestService requestService;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> getUserRequests() {
        try {
            User currentUser = getCurrentUser();
            List<RequestResponse> requests = requestService.getUserRequests(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", requests);
            response.put("count", requests.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching requests: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> getRequestsByStatus(@PathVariable String status) {
        try {
            List<RequestResponse> requests = requestService.getRequestsByStatus(status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", requests);
            response.put("count", requests.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching requests by status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> processRequest(@RequestBody RequestActionDTO actionDTO) {
        try {
            User currentUser = getCurrentUser();
            RequestResponse response = requestService.processRequest(actionDTO, currentUser);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Request processed successfully");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
}
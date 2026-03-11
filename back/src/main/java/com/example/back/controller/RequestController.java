package com.example.back.controller;

import com.example.back.entity.Application;
import com.example.back.entity.Convention;
import com.example.back.entity.Request;
import com.example.back.entity.User;
import com.example.back.payload.request.CreateReassignmentRequestDTO;
import com.example.back.payload.request.RequestActionDTO;
import com.example.back.payload.response.RequestResponse;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.RequestService;
import com.example.back.service.WorkloadService;
import com.example.back.service.mapper.RequestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/requests")
@Slf4j
public class RequestController {

    @Autowired
    private RequestService requestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private RequestMapper requestMapper;




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
            log.info("Fetching requests for user: {} (ID: {})", currentUser.getUsername(), currentUser.getId());

            List<RequestResponse> requests = requestService.getUserRequests(currentUser);

            log.info("Found {} requests for user {}", requests.size(), currentUser.getUsername());

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
            log.info("Processing request {} with action {} by user {}",
                    actionDTO.getRequestId(), actionDTO.getAction(), currentUser.getUsername());

            RequestResponse response;

            // Determine which type of request based on user role and request type
            // We'll let the service handle the logic based on the request ID

            response = requestService.processRequest(actionDTO, currentUser);

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

    @GetMapping("/chefs/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> getAvailableChefs(@RequestParam(required = false) Long applicationId) {
        try {
            List<User> chefs = userRepository.findByRoleName("ROLE_CHEF_PROJET");

            // Filter out current user if needed
            User currentUser = getCurrentUser();
            chefs = chefs.stream()
                    .filter(c -> !c.getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());

            // Get workload info
            List<Map<String, Object>> chefList = new ArrayList<>();
            for (User chef : chefs) {
                Map<String, Object> chefInfo = new HashMap<>();
                chefInfo.put("id", chef.getId());
                chefInfo.put("firstName", chef.getFirstName());
                chefInfo.put("lastName", chef.getLastName());
                chefInfo.put("email", chef.getEmail());

                // Get workload if applicationId provided
                if (applicationId != null) {
                    try {
                        WorkloadService.AssignmentCheck check = workloadService.checkAssignment(chef.getId(), applicationId);
                        chefInfo.put("currentWorkload", check.getAnalysis() != null ?
                                check.getAnalysis().getCurrentWorkload() : 0);
                        chefInfo.put("projectedWorkload", check.getAnalysis() != null ?
                                check.getAnalysis().getProjectedWorkload() : 0);
                        chefInfo.put("canAccept", check.isCanAssign());
                    } catch (Exception e) {
                        log.error("Error checking workload for chef {}: {}", chef.getId(), e.getMessage());
                    }
                }

                chefList.add(chefInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", chefList);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching available chefs: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // In RequestController.java - Add this new endpoint

    @PostMapping("/create-reassignment")
    @PreAuthorize("hasAnyRole('CHEF_PROJET')")
    public ResponseEntity<?> createReassignmentRequest(@RequestBody CreateReassignmentRequestDTO requestDTO) {
        try {
            User currentUser = getCurrentUser();
            log.info("========== CREATE REASSIGNMENT REQUEST ==========");
            log.info("Chef de projet: {} (ID: {})", currentUser.getUsername(), currentUser.getId());
            log.info("Application ID: {}", requestDTO.getApplicationId());
            log.info("Recommended Chef ID: {}", requestDTO.getRecommendedChefId());
            log.info("Reason: {}", requestDTO.getReason());

            // Get the application
            Application application = applicationRepository.findById(requestDTO.getApplicationId())
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Verify that the current user is the creator of this application
            if (application.getCreatedBy() == null || !application.getCreatedBy().getId().equals(currentUser.getId())) {
                log.error("Access denied: User {} is not the creator of application {}",
                        currentUser.getId(), application.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Vous n'êtes pas le créateur de cette application"));
            }

            // Get the recommended chef
            User recommendedChef = userRepository.findById(requestDTO.getRecommendedChefId())
                    .orElseThrow(() -> new RuntimeException("Recommended chef not found"));

            // Verify recommended chef has the correct role
            boolean isChef = recommendedChef.getRoles().stream()
                    .anyMatch(r -> r.getName().name().equals("ROLE_CHEF_PROJET"));

            if (!isChef) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("L'utilisateur recommandé n'est pas un chef de projet"));
            }

            // Get the convention (most recent active convention for this app)
            Convention convention = conventionRepository.findTopByApplicationAndArchivedFalseOrderByCreatedAtDesc(application)
                    .orElseThrow(() -> new RuntimeException("No active convention found for this application"));

            // Create the reassignment request
            Request request = requestService.createReassignmentRequestFromChef(
                    application,
                    convention,
                    currentUser,
                    recommendedChef,
                    requestDTO.getReason(),
                    requestDTO.getRecommendations()
            );

            RequestResponse response = requestMapper.toResponse(request);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Demande de réassignation créée avec succès");
            apiResponse.put("data", response);

            log.info("✅ Reassignment request created successfully with ID: {}", request.getId());
            log.info("========== END CREATE REASSIGNMENT REQUEST ==========");

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error creating reassignment request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
}

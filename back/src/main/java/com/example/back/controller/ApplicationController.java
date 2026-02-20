package com.example.back.controller;

import com.example.back.entity.Application;
import com.example.back.entity.Convention;
import com.example.back.entity.User;
import com.example.back.payload.request.ApplicationRequest;
import com.example.back.payload.response.ApplicationResponse;
import com.example.back.payload.response.ConventionResponse;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.ApplicationService;
import com.example.back.service.WorkloadService;
import com.example.back.service.mapper.ConventionMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
@Slf4j
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private ConventionMapper conventionMapper;

    @Autowired
    private WorkloadService workloadService;


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getAllApplications() {
        try {
            log.info("GET /api/applications called - Fetching all applications");
            List<ApplicationResponse> applications = applicationService.getAllApplications();

            log.info("Found {} applications", applications.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applications);
            response.put("count", applications.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching applications: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getApplicationById(@PathVariable Long id) {
        try {
            ApplicationResponse application = applicationService.getApplicationById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", application);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching application: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> createApplication(@Valid @RequestBody ApplicationRequest request) {
        try {
            ApplicationResponse application = applicationService.createApplication(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application created successfully");
            response.put("data", application);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating application: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> updateApplication(@PathVariable Long id,
                                               @Valid @RequestBody ApplicationRequest request) {
        try {
            ApplicationResponse application = applicationService.updateApplication(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application updated successfully");
            response.put("data", application);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating application: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> deleteApplication(@PathVariable Long id) {
        try {
            applicationService.deleteApplication(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting application: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/chef-projet/{chefDeProjetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> getApplicationsByChefDeProjet(@PathVariable Long chefDeProjetId) {
        try {
            List<ApplicationResponse> applications = applicationService.getApplicationsByChefDeProjet(chefDeProjetId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applications);
            response.put("count", applications.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching applications by chef de projet: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }



    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> searchApplications(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) Long chefDeProjetId,
            @RequestParam(required = false) String status) {

        try {
            List<ApplicationResponse> applications = applicationService.searchApplications(
                    code, name, clientName, chefDeProjetId, status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applications);
            response.put("count", applications.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching applications: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/calculate-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> calculateApplicationStatus(@PathVariable Long id) {
        try {
            applicationService.calculateApplicationStatus(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application status calculated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error calculating application status: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'DECIDEUR')")
    public ResponseEntity<?> getApplicationDashboard() {
        try {
            Map<String, Object> dashboard = applicationService.getApplicationDashboard();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dashboard);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching application dashboard: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}/client-structure")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> getClientStructureForApplication(@PathVariable Long id) {
        try {
            var structure = applicationService.getOrCreateStructureForApplication(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", structure);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting client structure for application: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    // Add new endpoint
    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> getUnassignedApplications() {
        try {
            log.info("GET /api/applications/unassigned called - Fetching unassigned applications");
            List<ApplicationResponse> applications = applicationService.getUnassignedApplications();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applications);
            response.put("count", applications.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching unassigned applications: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/generate-code")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> generateApplicationCode() {
        try {
            String suggestedCode = applicationService.generateSuggestedApplicationCode();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("suggestedCode", suggestedCode);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating application code: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }



    @GetMapping("/conventions/{chefDeProjetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> getConventionsByChefDeProjet(@PathVariable Long chefDeProjetId) {
        try {
            log.info("GET /api/applications/conventions/{} called", chefDeProjetId);

            // Verify the chef de projet exists
            User chefDeProjet = userRepository.findById(chefDeProjetId)
                    .orElseThrow(() -> new RuntimeException("Chef de projet not found"));

            // Get all applications for this chef de projet
            List<Application> applications = applicationRepository.findByChefDeProjet(chefDeProjet);

            if (applications.isEmpty()) {
                log.info("No applications found for chef de projet {}", chefDeProjetId);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", new ArrayList<>());
                response.put("count", 0);
                response.put("message", "No applications found for this chef de projet");
                return ResponseEntity.ok(response);
            }

            // Get conventions for these applications
            List<Convention> conventions = new ArrayList<>();
            for (Application application : applications) {
                // Use findByApplicationAndArchivedFalse or findByApplication if the method doesn't exist
                List<Convention> appConventions;

                // Try different repository methods
                try {
                    // Method 1: If you have a custom query method
                    appConventions = conventionRepository.findByApplicationAndArchivedFalse(application);
                } catch (Exception e) {
                    // Method 2: Fallback to general query
                    log.warn("Using fallback method for conventions query");
                    appConventions = conventionRepository.findByApplication(application);
                    // Filter out archived conventions
                    appConventions = appConventions.stream()
                            .filter(conv -> !conv.getArchived())
                            .collect(Collectors.toList());
                }

                conventions.addAll(appConventions);
                log.info("Found {} conventions for application {}", appConventions.size(), application.getId());
            }

            List<ConventionResponse> conventionResponses = conventions.stream()
                    .map(conventionMapper::toResponse)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conventionResponses);
            response.put("count", conventionResponses.size());
            response.put("message", String.format("Found %d conventions across %d applications",
                    conventionResponses.size(), applications.size()));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching conventions by chef de projet {}: {}", chefDeProjetId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch conventions: " + e.getMessage()));
        }
    }


    /**
     * Get date summary for an application (to see if dates are synced with conventions)
     */
    @GetMapping("/{id}/date-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getApplicationDateSummary(@PathVariable Long id) {
        try {
            Map<String, Object> summary = applicationService.getApplicationDateSummary(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", summary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting application date summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/without-conventions")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> getApplicationsWithoutConventions() {
        try {
            List<ApplicationResponse> applications = applicationService.getApplicationsWithoutConventions();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", applications);
            response.put("count", applications.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching applications without conventions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }


    @GetMapping("/workload/check")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_PROJET')")
    public ResponseEntity<?> checkAssignment(
            @RequestParam Long chefId,
            @RequestParam Long applicationId) {
        try {
            WorkloadService.AssignmentCheck check = workloadService.checkAssignment(chefId, applicationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", check);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking assignment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/workload/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_PROJET')")
    public ResponseEntity<?> getWorkloadDashboard() {
        try {
            WorkloadService.WorkloadDashboard dashboard = workloadService.getWorkloadDashboard();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dashboard);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching workload dashboard: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/workload/assign")
    @PreAuthorize("hasAnyRole('ADMIN','CHEF_PROJET')")
    public ResponseEntity<?> assignWithWorkloadCheck(
            @RequestParam Long chefId,
            @RequestParam Long applicationId,
            @RequestParam(defaultValue = "false") boolean force) {
        try {
            WorkloadService.AssignmentResult result = workloadService.assignApplication(
                    chefId,
                    applicationId,
                    force
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("warning", result.isWarning());
            response.put("blocked", result.isBlocked());
            response.put("message", result.getMessage());
            response.put("data", result.getCheck());
            response.put("updatedWorkload", result.getUpdatedWorkload());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error assigning with workload check: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> manuallyTerminateApplication(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String reason = request != null ? request.get("reason") : null;
            ApplicationResponse application = applicationService.manuallyTerminateApplication(id, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Application marquée comme terminée avec succès");

            // Add termination info to response
            Map<String, Object> terminationInfo = new HashMap<>();
            terminationInfo.put("terminatedAt", application.getTerminatedAt());
            terminationInfo.put("terminatedBy", application.getTerminatedBy());
            terminationInfo.put("daysRemaining", application.getDaysRemainingAtTermination());
            terminationInfo.put("isEarly", application.isTerminatedEarly());
            terminationInfo.put("isOnTime", application.isTerminatedOnTime());
            terminationInfo.put("isLate", application.isTerminatedLate());
            terminationInfo.put("info", application.getTerminationInfo());

            response.put("terminationInfo", terminationInfo);
            response.put("data", application);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error terminating application: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

}
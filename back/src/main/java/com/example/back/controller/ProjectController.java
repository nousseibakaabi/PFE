// ProjectController.java
package com.example.back.controller;

import com.example.back.payload.request.ProjectRequest;
import com.example.back.payload.response.ProjectResponse;
import com.example.back.service.ProjectService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@Slf4j
public class ProjectController {

    @Autowired
    private ProjectService projectService;


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getAllProjects() {
        try {
            log.info("GET /api/projects called - Fetching all projects");
            List<ProjectResponse> projects = projectService.getAllProjects();

            log.info("Found {} projects", projects.size()); // Debug log

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", projects);
            response.put("count", projects.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching projects: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getProjectById(@PathVariable Long id) {
        try {
            ProjectResponse project = projectService.getProjectById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", project);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching project: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> createProject(@Valid @RequestBody ProjectRequest request) {
        try {
            ProjectResponse project = projectService.createProject(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project created successfully");
            response.put("data", project);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating project: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> updateProject(@PathVariable Long id,
                                           @Valid @RequestBody ProjectRequest request) {
        try {
            ProjectResponse project = projectService.updateProject(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project updated successfully");
            response.put("data", project);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating project: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        try {
            projectService.deleteProject(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting project: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/chef-projet/{chefDeProjetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> getProjectsByChefDeProjet(@PathVariable Long chefDeProjetId) {
        try {
            List<ProjectResponse> projects = projectService.getProjectsByChefDeProjet(chefDeProjetId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", projects);
            response.put("count", projects.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching projects by chef de projet: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> searchProjects(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) Long chefDeProjetId,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) String status) {

        try {
            List<ProjectResponse> projects = projectService.searchProjects(
                    code, name, clientName, chefDeProjetId, applicationId, status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", projects);
            response.put("count", projects.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching projects: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> updateProjectProgress(@PathVariable Long id,
                                                   @RequestParam Integer progress) {
        try {
            ProjectResponse project = projectService.updateProjectProgress(id, progress);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project progress updated successfully");
            response.put("data", project);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating project progress: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/calculate-progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> calculateProjectProgress(@PathVariable Long id) {
        try {
            projectService.calculateProjectProgress(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Project progress calculated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error calculating project progress: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'DECIDEUR')")
    public ResponseEntity<?> getProjectDashboard() {
        try {
            Map<String, Object> dashboard = projectService.getProjectDashboard();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dashboard);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching project dashboard: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}/client-structure")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER')")
    public ResponseEntity<?> getClientStructureForProject(@PathVariable Long id) {
        try {
            var structure = projectService.getOrCreateStructureForProject(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", structure);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting client structure for project: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    // ProjectController.java - Add new endpoint
    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET')")
    public ResponseEntity<?> getUnassignedProjects() {
        try {
            log.info("GET /api/projects/unassigned called - Fetching unassigned projects");
            List<ProjectResponse> projects = projectService.getUnassignedProjects();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", projects);
            response.put("count", projects.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching unassigned projects: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Add endpoint to assign chef de projet to project
    @PutMapping("/{id}/assign-chef/{chefDeProjetId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignChefDeProjetToProject(@PathVariable Long id,
                                                         @PathVariable Long chefDeProjetId) {
        try {
            ProjectResponse project = projectService.assignChefDeProjet(id, chefDeProjetId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Chef de projet assigned successfully");
            response.put("data", project);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error assigning chef de projet: ", e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
}
// ProjectService.java
package com.example.back.service;

import com.example.back.entity.Project;
import com.example.back.entity.Application;
import com.example.back.entity.User;
import com.example.back.entity.Structure;
import com.example.back.payload.request.ProjectRequest;
import com.example.back.payload.response.ProjectResponse;
import com.example.back.repository.ProjectRepository;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.UserRepository;
import com.example.back.repository.StructureRepository;
import com.example.back.repository.ConventionRepository;
import com.example.back.service.mapper.ProjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * Create a new project
     */

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        try {
            log.info("Creating project with code: {}", request.getCode());

            // Check if code already exists
            if (projectRepository.existsByCode(request.getCode())) {
                throw new RuntimeException("Project with this code already exists");
            }

            // Check if name already exists
            if (projectRepository.existsByName(request.getName())) {
                throw new RuntimeException("Project with this name already exists");
            }

            // Fetch application
            Application application = applicationRepository.findById(request.getApplicationId())
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Fetch chef de projet - make it optional
            User chefDeProjet = null;
            if (request.getChefDeProjetId() != null) {
                chefDeProjet = userRepository.findById(request.getChefDeProjetId())
                        .orElseThrow(() -> new RuntimeException("Chef de Projet not found"));

                // Verify chef de projet has the correct role
                if (!chefDeProjet.getRoles().stream()
                        .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET"))) {
                    throw new RuntimeException("Selected user is not a Chef de Projet");
                }
            }

            // Create project
            Project project = new Project();
            project.setCode(request.getCode());
            project.setName(request.getName());
            project.setDescription(request.getDescription());
            project.setApplication(application);
            project.setChefDeProjet(chefDeProjet); // Can be null
            project.setClientName(request.getClientName());
            project.setClientEmail(request.getClientEmail());
            project.setClientPhone(request.getClientPhone());
            project.setClientAddress(request.getClientAddress());
            project.setDateDebut(request.getDateDebut());
            project.setDateFin(request.getDateFin());
            project.setProgress(request.getProgress() != null ? request.getProgress() : 0);
            project.setBudget(request.getBudget());
            project.setStatus(request.getStatus() != null ? request.getStatus() : "PLANIFIE");

            Project savedProject = projectRepository.save(project);
            log.info("Project created successfully: {}", savedProject.getCode());

            return projectMapper.toResponse(savedProject);

        } catch (RuntimeException e) {
            log.error("Error creating project: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating project: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create project: " + e.getMessage());
        }
    }
    /**
     * Get project by ID with access control
     */
    public ProjectResponse getProjectById(Long id) {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check access: Admin sees all, Chef de Projet sees only their projects
            if (!"ROLE_ADMIN".equals(currentRole)) {
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (!project.getChefDeProjet().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Access denied: You can only view your own projects");
                }
            }

            return projectMapper.toResponse(project);

        } catch (RuntimeException e) {
            log.error("Error fetching project: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching project: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch project: " + e.getMessage());
        }
    }

    /**
     * Update project
     */
    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check access
            if (!"ROLE_ADMIN".equals(currentRole)) {
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (project.getChefDeProjet() == null ||
                        !project.getChefDeProjet().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Access denied: You can only update your own projects");
                }
            }

            // Check if new code conflicts (if changed)
            if (!project.getCode().equals(request.getCode()) &&
                    projectRepository.existsByCode(request.getCode())) {
                throw new RuntimeException("Project with this code already exists");
            }

            // Check if new name conflicts (if changed)
            if (!project.getName().equals(request.getName()) &&
                    projectRepository.existsByName(request.getName())) {
                throw new RuntimeException("Project with this name already exists");
            }

            // Update fields
            project.setCode(request.getCode());
            project.setName(request.getName());
            project.setDescription(request.getDescription());
            project.setClientName(request.getClientName());
            project.setClientEmail(request.getClientEmail());
            project.setClientPhone(request.getClientPhone());
            project.setClientAddress(request.getClientAddress());
            project.setDateDebut(request.getDateDebut());
            project.setDateFin(request.getDateFin());
            project.setBudget(request.getBudget());
            project.setStatus(request.getStatus());

            // Progress can be updated by both Admin and Chef de Projet
            if (request.getProgress() != null) {
                project.setProgress(request.getProgress());
            }

            // Only Admin can change application and chef de projet
            if ("ROLE_ADMIN".equals(currentRole)) {
                Application application = applicationRepository.findById(request.getApplicationId())
                        .orElseThrow(() -> new RuntimeException("Application not found"));
                project.setApplication(application);

                // Handle chef de projet - can be set to null
                if (request.getChefDeProjetId() != null) {
                    User chefDeProjet = userRepository.findById(request.getChefDeProjetId())
                            .orElseThrow(() -> new RuntimeException("Chef de Projet not found"));

                    // Verify role if assigning
                    if (!chefDeProjet.getRoles().stream()
                            .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET"))) {
                        throw new RuntimeException("Selected user is not a Chef de Projet");
                    }
                    project.setChefDeProjet(chefDeProjet);
                } else {
                    // Set to null if no chef de projet specified
                    project.setChefDeProjet(null);
                }
            }

            Project updatedProject = projectRepository.save(project);
            log.info("Project updated successfully: {}", updatedProject.getCode());

            return projectMapper.toResponse(updatedProject);

        } catch (RuntimeException e) {
            log.error("Error updating project: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating project: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update project: " + e.getMessage());
        }
    }

    /**
     * Delete project (only Admin)
     */
    @Transactional
    public void deleteProject(Long id) {
        try {
            Project project = projectRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check if project has conventions
            if (projectRepository.hasConventions(id)) {
                throw new RuntimeException("Cannot delete project that has conventions. Delete conventions first.");
            }

            projectRepository.delete(project);
            log.info("Project deleted successfully: {}", project.getCode());

        } catch (RuntimeException e) {
            log.error("Error deleting project: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting project: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete project: " + e.getMessage());
        }
    }

    /**
     * Get all projects with access control
     */
    public List<ProjectResponse> getAllProjects() {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            log.info("Fetching projects for user: {} with role: {}", currentUsername, currentRole);

            // For convention module, ALL authorized users should see ALL projects
            // because they need to be able to select any project to create a convention
            List<Project> projects = projectRepository.findAll();

            log.info("Returning {} projects to user {}", projects.size(), currentUsername);

            return projects.stream()
                    .map(projectMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching projects: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch projects: " + e.getMessage());
        }
    }

    /**
     * Get projects by Chef de Projet
     */
    public List<ProjectResponse> getProjectsByChefDeProjet(Long chefDeProjetId) {
        try {
            List<Project> projects = projectRepository.findByChefDeProjetId(chefDeProjetId);
            return projects.stream()
                    .map(projectMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching projects by chef de projet: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch projects");
        }
    }

    /**
     * Search projects with filters
     */
    public List<ProjectResponse> searchProjects(
            String code, String name, String clientName,
            Long chefDeProjetId, Long applicationId, String status) {

        try {
            String currentRole = getCurrentUserRole();

            // If not admin, restrict to their projects
            if (!"ROLE_ADMIN".equals(currentRole)) {
                User currentUser = userRepository.findByUsername(getCurrentUsername())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                chefDeProjetId = currentUser.getId();
            }

            List<Project> projects = projectRepository.searchProjects(
                    code, name, clientName, chefDeProjetId, applicationId, status);

            return projects.stream()
                    .map(projectMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching projects: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search projects");
        }
    }

    /**
     * Update project progress
     */
    @Transactional
    public ProjectResponse updateProjectProgress(Long projectId, Integer progress) {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Validate progress
            if (progress < 0 || progress > 100) {
                throw new RuntimeException("Progress must be between 0 and 100");
            }

            // Check access - allow progress updates for:
            // 1. Admin (all projects)
            // 2. Chef de Projet (their own projects)
            // 3. Commercial Metier (all projects for billing/conventions)
            if (!"ROLE_ADMIN".equals(currentRole) && !"ROLE_COMMERCIAL_METIER".equals(currentRole)) {
                if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                    User currentUser = userRepository.findByUsername(currentUsername)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    if (!project.getChefDeProjet().getId().equals(currentUser.getId())) {
                        throw new RuntimeException("Access denied: You can only update your own projects");
                    }
                } else {
                    throw new RuntimeException("Access denied: You don't have permission to update projects");
                }
            }

            project.setProgress(progress);

            // Update status if progress is 100%
            if (progress == 100) {
                project.setStatus("TERMINE");
            } else if ("TERMINE".equals(project.getStatus())) {
                project.setStatus("EN_COURS");
            }

            Project updatedProject = projectRepository.save(project);
            log.info("User {} updated progress for project {}: {}%",
                    currentUsername, project.getCode(), progress);

            return projectMapper.toResponse(updatedProject);

        } catch (RuntimeException e) {
            log.error("Error updating project progress: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating project progress: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update project progress");
        }
    }

    /**
     * Calculate and update project progress automatically
     */
    @Transactional
    public void calculateProjectProgress(Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            project.calculateProgress();
            project.updateStatusBasedOnDates();

            projectRepository.save(project);
            log.info("Calculated progress for project {}: {}%", project.getCode(), project.getProgress());

        } catch (Exception e) {
            log.error("Error calculating project progress: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate project progress");
        }
    }

    /**
     * Get project dashboard statistics
     */
    public Map<String, Object> getProjectDashboard() {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            List<Project> projects;

            if ("ROLE_ADMIN".equals(currentRole)) {
                projects = projectRepository.findAll();
            } else {
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                projects = projectRepository.findByChefDeProjetId(currentUser.getId());
            }

            Map<String, Object> stats = new HashMap<>();

            // Basic counts
            stats.put("totalProjects", projects.size());
            stats.put("activeProjects", projects.stream().filter(Project::isActive).count());
            stats.put("plannedProjects", projects.stream().filter(p -> "PLANIFIE".equals(p.getStatus())).count());
            stats.put("completedProjects", projects.stream().filter(p -> "TERMINE".equals(p.getStatus())).count());
            stats.put("delayedProjects", projects.stream().filter(Project::isDelayed).count());

            // Progress statistics
            double avgProgress = projects.stream()
                    .mapToInt(p -> p.getProgress() != null ? p.getProgress() : 0)
                    .average()
                    .orElse(0.0);
            stats.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);

            // Budget statistics
            double totalBudget = projects.stream()
                    .mapToDouble(p -> p.getBudget() != null ? p.getBudget() : 0)
                    .sum();
            stats.put("totalBudget", totalBudget);

            // Convention statistics
            int totalConventions = projects.stream()
                    .mapToInt(Project::getConventionsCount)
                    .sum();
            stats.put("totalConventions", totalConventions);

            // Recent activity
            List<Project> recentProjects = projects.stream()
                    .sorted((p1, p2) -> p2.getUpdatedAt().compareTo(p1.getUpdatedAt()))
                    .limit(5)
                    .collect(Collectors.toList());
            stats.put("recentProjects", recentProjects.stream()
                    .map(projectMapper::toResponse)
                    .collect(Collectors.toList()));

            // Projects ending soon - FIXED: Pass the end date parameter
            LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
            List<Project> endingSoon = projectRepository.findProjectsEndingSoon(thirtyDaysFromNow);
            stats.put("endingSoon", endingSoon.stream()
                    .map(projectMapper::toResponse)
                    .collect(Collectors.toList()));

            return stats;

        } catch (Exception e) {
            log.error("Error getting project dashboard: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get project dashboard");
        }
    }

    /**
     * Find or create Structure based on project client name
     * This is used when creating a convention for a project
     */
    public Structure getOrCreateStructureForProject(Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check if structure already exists with this client name
            Optional<Structure> existingStructure = structureRepository
                    .findByName(project.getClientName());

            if (existingStructure.isPresent()) {
                return existingStructure.get();
            }

            // Create new structure for this client
            Structure newStructure = new Structure();
            newStructure.setCode(generateClientCode(project.getClientName()));
            newStructure.setName(project.getClientName());
            newStructure.setEmail(project.getClientEmail());
            newStructure.setPhone(project.getClientPhone());
            newStructure.setAddress(project.getClientAddress());
            newStructure.setTypeStructure("CLIENT");

            return structureRepository.save(newStructure);

        } catch (Exception e) {
            log.error("Error getting/creating structure for project: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get structure for project");
        }
    }

    private String generateClientCode(String clientName) {
        String code = clientName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(8, clientName.length()));

        // Add timestamp to make it unique
        return "CLI-" + code + "-" + System.currentTimeMillis() % 10000;
    }

    /**
     * Get current authenticated user
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    /**
     * Get current user role
     */
    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .orElse("ROLE_USER");
    }

    // ProjectService.java - Add new methods
    public List<ProjectResponse> getUnassignedProjects() {
        try {
            List<Project> projects = projectRepository.findByChefDeProjetIsNull();
            return projects.stream()
                    .map(projectMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching unassigned projects: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch unassigned projects");
        }
    }

    @Transactional
    public ProjectResponse assignChefDeProjet(Long projectId, Long chefDeProjetId) {
        try {
            // Check if current user is admin
            String currentRole = getCurrentUserRole();
            if (!"ROLE_ADMIN".equals(currentRole)) {
                throw new RuntimeException("Only admin can assign chef de projet");
            }

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            User chefDeProjet = userRepository.findById(chefDeProjetId)
                    .orElseThrow(() -> new RuntimeException("Chef de Projet not found"));

            // Verify chef de projet has the correct role
            if (!chefDeProjet.getRoles().stream()
                    .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET"))) {
                throw new RuntimeException("Selected user is not a Chef de Projet");
            }

            project.setChefDeProjet(chefDeProjet);
            Project updatedProject = projectRepository.save(project);

            log.info("Chef de projet {} assigned to project {}",
                    chefDeProjet.getUsername(), project.getCode());

            return projectMapper.toResponse(updatedProject);

        } catch (RuntimeException e) {
            log.error("Error assigning chef de projet: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error assigning chef de projet: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign chef de projet");
        }
    }


}
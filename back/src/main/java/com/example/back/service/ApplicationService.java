package com.example.back.service;

import com.example.back.entity.Application;
import com.example.back.entity.User;
import com.example.back.entity.Structure;
import com.example.back.payload.request.ApplicationRequest;
import com.example.back.payload.response.ApplicationResponse;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.UserRepository;
import com.example.back.repository.StructureRepository;
import com.example.back.repository.ConventionRepository;
import com.example.back.service.mapper.ApplicationMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private ApplicationMapper applicationMapper;

    /**
     * Create a new application
     */
    @Transactional
    public ApplicationResponse createApplication(ApplicationRequest request) {
        try {
            log.info("Creating application with code: {}", request.getCode());

            // Check if code already exists
            if (applicationRepository.existsByCode(request.getCode())) {
                throw new RuntimeException("Application with this code already exists");
            }

            // Check if name already exists
            if (applicationRepository.existsByName(request.getName())) {
                throw new RuntimeException("Application with this name already exists");
            }

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

            // Create application
            Application application = new Application();
            application.setCode(request.getCode());
            application.setName(request.getName());
            application.setDescription(request.getDescription());
            application.setChefDeProjet(chefDeProjet); // Can be null
            application.setClientName(request.getClientName());
            application.setClientEmail(request.getClientEmail());
            application.setClientPhone(request.getClientPhone());
            application.setDateDebut(request.getDateDebut());
            application.setDateFin(request.getDateFin());
            application.setMaxUser(request.getMaxUser());
            application.setMinUser(request.getMinUser());
            application.setStatus(request.getStatus() != null ? request.getStatus() : "PLANIFIE");

            Application savedApplication = applicationRepository.save(application);
            log.info("Application created successfully: {}", savedApplication.getCode());

            return applicationMapper.toResponse(savedApplication);

        } catch (RuntimeException e) {
            log.error("Error creating application: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create application: " + e.getMessage());
        }
    }

    /**
     * Get application by ID with access control
     */
    public ApplicationResponse getApplicationById(Long id) {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            Application application = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Check access based on role
            if ("ROLE_ADMIN".equals(currentRole)) {
                // Admin sees all
                return applicationMapper.toResponse(application);
            }
            else if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                // Chef de projet only sees their own applications
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (application.getChefDeProjet() == null ||
                        !application.getChefDeProjet().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Access denied: You can only view your own applications");
                }
                return applicationMapper.toResponse(application);
            }
            else {
                // DECIDEUR and COMMERCIAL_METIER can view all applications
                return applicationMapper.toResponse(application);
            }

        } catch (RuntimeException e) {
            log.error("Error fetching application: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch application: " + e.getMessage());
        }
    }


    /**
     * Update application
     */
    @Transactional
    public ApplicationResponse updateApplication(Long id, ApplicationRequest request) {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            Application application = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Check access based on role
            if ("ROLE_ADMIN".equals(currentRole)) {
                // Admin can update all applications
                log.info("Admin updating application: {}", application.getCode());
            }
            else if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                // Chef de projet can only update their own applications
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (application.getChefDeProjet() == null ||
                        !application.getChefDeProjet().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Access denied: You can only update your own applications");
                }
            }
            else {
                // Other roles cannot update applications
                throw new RuntimeException("Access denied: You don't have permission to update applications");
            }

            // Check if new code conflicts (if changed)
            if (!application.getCode().equals(request.getCode()) &&
                    applicationRepository.existsByCode(request.getCode())) {
                throw new RuntimeException("Application with this code already exists");
            }

            // Check if new name conflicts (if changed)
            if (!application.getName().equals(request.getName()) &&
                    applicationRepository.existsByName(request.getName())) {
                throw new RuntimeException("Application with this name already exists");
            }

            // Update fields
            application.setCode(request.getCode());
            application.setName(request.getName());
            application.setDescription(request.getDescription());
            application.setClientName(request.getClientName());
            application.setClientEmail(request.getClientEmail());
            application.setClientPhone(request.getClientPhone());
            application.setDateDebut(request.getDateDebut());
            application.setDateFin(request.getDateFin());
            application.setStatus(request.getStatus());
            application.setMaxUser(request.getMaxUser());
            application.setMinUser(request.getMinUser());

            // Only Admin can change chef de projet
            if ("ROLE_ADMIN".equals(currentRole)) {
                if (request.getChefDeProjetId() != null) {
                    User chefDeProjet = userRepository.findById(request.getChefDeProjetId())
                            .orElseThrow(() -> new RuntimeException("Chef de Projet not found"));

                    if (!chefDeProjet.getRoles().stream()
                            .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET"))) {
                        throw new RuntimeException("Selected user is not a Chef de Projet");
                    }
                    application.setChefDeProjet(chefDeProjet);
                } else {
                    application.setChefDeProjet(null);
                }
            }

            Application updatedApplication = applicationRepository.save(application);
            log.info("Application updated successfully: {}", updatedApplication.getCode());

            return applicationMapper.toResponse(updatedApplication);

        } catch (RuntimeException e) {
            log.error("Error updating application: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update application: " + e.getMessage());
        }
    }


    /**
     * Delete application with access control
     */
    @Transactional
    public void deleteApplication(Long id) {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            Application application = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Check access based on role
            if ("ROLE_ADMIN".equals(currentRole)) {
                // Admin can delete all applications
                log.info("Admin deleting application: {}", application.getCode());
            }
            else if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                // Chef de projet can only delete their own applications
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (application.getChefDeProjet() == null ||
                        !application.getChefDeProjet().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Access denied: You can only delete your own applications");
                }
            }
            else {
                // Other roles cannot delete applications
                throw new RuntimeException("Access denied: You don't have permission to delete applications");
            }

            // Check if application has conventions
            if (applicationRepository.hasConventions(id)) {
                throw new RuntimeException("Cannot delete application that has conventions. Delete conventions first.");
            }

            applicationRepository.delete(application);
            log.info("Application deleted successfully: {}", application.getCode());

        } catch (RuntimeException e) {
            log.error("Error deleting application: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete application: " + e.getMessage());
        }
    }


    /**
     * Get all applications with access control
     */
    public List<ApplicationResponse> getAllApplications() {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            log.info("Fetching applications for user: {} with role: {}", currentUsername, currentRole);

            List<Application> applications;

            // ADMIN sees all applications
            if ("ROLE_ADMIN".equals(currentRole)) {
                applications = applicationRepository.findAll();
            }
            // CHEF_PROJET sees only their own applications
            else if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                applications = applicationRepository.findByChefDeProjetId(currentUser.getId());
            }
            // DECIDEUR and COMMERCIAL_METIER see all applications (for viewing and convention purposes)
            else {
                applications = applicationRepository.findAll();
            }

            log.info("Returning {} applications to user {}", applications.size(), currentUsername);

            return applications.stream()
                    .map(applicationMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching applications: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch applications: " + e.getMessage());
        }
    }

    /**
     * Get applications by Chef de Projet
     */
    public List<ApplicationResponse> getApplicationsByChefDeProjet(Long chefDeProjetId) {
        try {
            List<Application> applications = applicationRepository.findByChefDeProjetId(chefDeProjetId);
            return applications.stream()
                    .map(applicationMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching applications by chef de projet: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch applications");
        }
    }

    /**
     * Search applications with filters
     */
    public List<ApplicationResponse> searchApplications(
            String code, String name, String clientName,
            Long chefDeProjetId, String status) {

        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            Long effectiveChefDeProjetId = chefDeProjetId;

            // If not admin and not decideur/commercial, restrict to their applications
            if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                effectiveChefDeProjetId = currentUser.getId();
            }
            // For admin, decideur, commercial - allow searching all (keep original chefDeProjetId param)

            List<Application> applications = applicationRepository.searchApplications(
                    code, name, clientName, effectiveChefDeProjetId, status);

            return applications.stream()
                    .map(applicationMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching applications: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search applications");
        }
    }


    /**
     * Calculate and update application status automatically
     */
    @Transactional
    public void calculateApplicationStatus(Long applicationId) {
        try {
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            application.updateStatusBasedOnDates();

            applicationRepository.save(application);
            log.info("Calculated status for application {}: {}", application.getCode(), application.getStatus());

        } catch (Exception e) {
            log.error("Error calculating application status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate application status");
        }
    }

    /**
     * Get application dashboard statistics
     */
    public Map<String, Object> getApplicationDashboard() {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            List<Application> applications;

            if ("ROLE_ADMIN".equals(currentRole)) {
                applications = applicationRepository.findAll();
            } else {
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                applications = applicationRepository.findByChefDeProjetId(currentUser.getId());
            }

            Map<String, Object> stats = new HashMap<>();

            // Basic counts
            stats.put("totalApplications", applications.size());
            stats.put("activeApplications", applications.stream().filter(Application::isActive).count());
            stats.put("plannedApplications", applications.stream().filter(a -> "PLANIFIE".equals(a.getStatus())).count());
            stats.put("completedApplications", applications.stream().filter(a -> "TERMINE".equals(a.getStatus())).count());



            // Convention statistics
            int totalConventions = applications.stream()
                    .mapToInt(Application::getConventionsCount)
                    .sum();
            stats.put("totalConventions", totalConventions);

            // Recent activity
            List<Application> recentApplications = applications.stream()
                    .sorted((a1, a2) -> a2.getUpdatedAt().compareTo(a1.getUpdatedAt()))
                    .limit(5)
                    .collect(Collectors.toList());
            stats.put("recentApplications", recentApplications.stream()
                    .map(applicationMapper::toResponse)
                    .collect(Collectors.toList()));

            // Applications ending soon
            LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
            List<Application> endingSoon = applicationRepository.findApplicationsEndingSoon(thirtyDaysFromNow);
            stats.put("endingSoon", endingSoon.stream()
                    .map(applicationMapper::toResponse)
                    .collect(Collectors.toList()));

            return stats;

        } catch (Exception e) {
            log.error("Error getting application dashboard: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get application dashboard");
        }
    }

    /**
     * Find or create Structure based on application client name
     * This is used when creating a convention for an application
     */
    public Structure getOrCreateStructureForApplication(Long applicationId) {
        try {
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Check if structure already exists with this client name
            Optional<Structure> existingStructure = structureRepository
                    .findByName(application.getClientName());

            if (existingStructure.isPresent()) {
                return existingStructure.get();
            }

            // Create new structure for this client
            Structure newStructure = new Structure();
            newStructure.setCode(generateClientCode(application.getClientName()));
            newStructure.setName(application.getClientName());
            newStructure.setEmail(application.getClientEmail());
            newStructure.setPhone(application.getClientPhone());
            newStructure.setTypeStructure("CLIENT");

            return structureRepository.save(newStructure);

        } catch (Exception e) {
            log.error("Error getting/creating structure for application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get structure for application");
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

    public List<ApplicationResponse> getUnassignedApplications() {
        try {
            List<Application> applications = applicationRepository.findByChefDeProjetIsNull();
            return applications.stream()
                    .map(applicationMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching unassigned applications: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch unassigned applications");
        }
    }

    @Transactional
    public ApplicationResponse assignChefDeProjet(Long applicationId, Long chefDeProjetId) {
        try {
            // Check if current user is admin
            String currentRole = getCurrentUserRole();
            if (!"ROLE_ADMIN".equals(currentRole)) {
                throw new RuntimeException("Only admin can assign chef de projet");
            }

            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            User chefDeProjet = userRepository.findById(chefDeProjetId)
                    .orElseThrow(() -> new RuntimeException("Chef de Projet not found"));

            // Verify chef de projet has the correct role
            if (!chefDeProjet.getRoles().stream()
                    .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET"))) {
                throw new RuntimeException("Selected user is not a Chef de Projet");
            }

            application.setChefDeProjet(chefDeProjet);
            Application updatedApplication = applicationRepository.save(application);

            log.info("Chef de projet {} assigned to application {}",
                    chefDeProjet.getUsername(), application.getCode());

            return applicationMapper.toResponse(updatedApplication);

        } catch (RuntimeException e) {
            log.error("Error assigning chef de projet: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error assigning chef de projet: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign chef de projet");
        }
    }

    /**
     * Generate suggested application code
     * Format: APP-YYYY-XXX where XXX is auto-incremented sequence
     */
    public String generateSuggestedApplicationCode() {
        int currentYear = LocalDate.now().getYear();
        String yearStr = String.valueOf(currentYear);

        // Get all used sequences for the current year
        List<Integer> usedSequences = applicationRepository.findUsedSequencesByYear(yearStr);

        // If no applications for this year, start with 001
        if (usedSequences == null || usedSequences.isEmpty()) {
            return String.format("APP-%d-%03d", currentYear, 1);
        }

        // Sort sequences
        Collections.sort(usedSequences);

        // Find the first available gap
        int expectedSequence = 1; // Start from 001

        for (int usedSequence : usedSequences) {
            if (usedSequence > expectedSequence) {
                // Found a gap! Return the missing number
                break;
            }
            expectedSequence = usedSequence + 1;
        }

        // If we reached beyond 999, start from 1 again (though unlikely)
        if (expectedSequence > 999) {
            // Find first available from the beginning
            expectedSequence = findFirstMissingSequence(usedSequences);
        }

        // Format with leading zeros
        return String.format("APP-%d-%03d", currentYear, expectedSequence);
    }

    // Helper method to find first missing number in a sorted list
    private int findFirstMissingSequence(List<Integer> sequences) {
        Set<Integer> sequenceSet = new HashSet<>(sequences);

        for (int i = 1; i <= 999; i++) {
            if (!sequenceSet.contains(i)) {
                return i;
            }
        }

        // If all numbers 1-999 are used (highly unlikely), return 1000
        return 1000;
    }
}
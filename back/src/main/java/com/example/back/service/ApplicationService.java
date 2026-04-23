package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.ApplicationRequest;
import com.example.back.payload.response.ApplicationResponse;
import com.example.back.repository.*;
import com.example.back.service.mapper.ApplicationMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApplicationService {


    private final EntitySyncService entitySyncService;


    private final ApplicationRepository applicationRepository;

    private final UserRepository userRepository;

    private final StructureRepository structureRepository;

    private final ConventionRepository conventionRepository;

    private final ApplicationMapper applicationMapper;

    private final HistoryService historyService;

    private final WorkloadService workloadService;

    public ApplicationService(EntitySyncService entitySyncService, ApplicationRepository applicationRepository, UserRepository userRepository, StructureRepository structureRepository, ConventionRepository conventionRepository, ApplicationMapper applicationMapper, HistoryService historyService , WorkloadService workloadService) {
        this.entitySyncService = entitySyncService;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.structureRepository = structureRepository;
        this.conventionRepository = conventionRepository;
        this.applicationMapper = applicationMapper;
        this.historyService = historyService;
        this.workloadService = workloadService;
    }


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

            // Get current user FIRST
            User currentUser = getCurrentUser();
            log.info("Current user: {}", currentUser != null ? currentUser.getUsername() : "null");

            // Fetch chef de projet - make it optional
            User chefDeProjet = null;
            if (request.getChefDeProjetId() != null && request.getChefDeProjetId() > 0) {
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

            // Set the createdBy field
            application.setCreatedBy(currentUser);

            Application savedApplication = applicationRepository.save(application);
            log.info("Application created successfully: {}", savedApplication.getCode());

            // LOG HISTORY: Application creation (use the same currentUser)
            historyService.logApplicationCreate(savedApplication, currentUser);

            // ===== IF CHEF WAS ASSIGNED DURING CREATION, USE WORKLOAD SERVICE =====
            if (chefDeProjet != null && currentUser != null) {
                log.info("Chef de projet assigned during creation - using workload service");

                // Call workload service to handle assignment with validation and notification
                WorkloadService.AssignmentResult result = workloadService.assignApplication(
                        chefDeProjet.getId(),
                        savedApplication.getId(),
                        false // force = false by default
                );

                if (result.isSuccess()) {
                    log.info("Workload validation passed for chef: {}", chefDeProjet.getUsername());
                } else if (result.isWarning()) {
                    log.warn("Workload warning for chef: {} - {}", chefDeProjet.getUsername(), result.getMessage());
                } else if (result.isBlocked()) {
                    log.error("Workload blocked for chef: {} - {}", chefDeProjet.getUsername(), result.getMessage());
                    throw new RuntimeException("Cannot assign chef due to workload limits: " + result.getMessage());
                }
            }

            return applicationMapper.toResponse(savedApplication);

        } catch (RuntimeException e) {
            log.error("Error creating application: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create application: " + e.getMessage());
        }
    }


    @Transactional
    public ApplicationResponse updateApplication(Long id, ApplicationRequest request) {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            Application application = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            // Store old values for history
            Application oldApplication = cloneApplication(application);

            // Track if chef changed
            Long oldChefId = application.getChefDeProjet() != null ?
                    application.getChefDeProjet().getId() : null;
            Long newChefId = request.getChefDeProjetId() != null && request.getChefDeProjetId() > 0 ?
                    request.getChefDeProjetId() : null;

            boolean chefChanged = (oldChefId == null && newChefId != null) ||
                    (oldChefId != null && !oldChefId.equals(newChefId));

            // Check access based on role
            if ("ROLE_ADMIN".equals(currentRole)) {
                // Admin can update all applications
                log.info("Admin updating application: {}", application.getCode());
            } else if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                // Chef de projet can only update their own applications
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (application.getChefDeProjet() == null ||
                        !application.getChefDeProjet().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Access denied: You can only update your own applications");
                }

                // Chef de projet cannot change the chef assignment
                if (chefChanged) {
                    throw new RuntimeException("Access denied: You cannot change the chef de projet assignment");
                }
            } else {
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

            User newChef = null;

            // Only Admin can change chef de projet
            if ("ROLE_ADMIN".equals(currentRole)) {
                if (request.getChefDeProjetId() != null && request.getChefDeProjetId() > 0) {
                    newChef = userRepository.findById(request.getChefDeProjetId())
                            .orElseThrow(() -> new RuntimeException("Chef de Projet not found"));

                    if (!newChef.getRoles().stream()
                            .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET"))) {
                        throw new RuntimeException("Selected user is not a Chef de Projet");
                    }
                    application.setChefDeProjet(newChef);
                } else {
                    application.setChefDeProjet(null);
                }
            }

            Application updatedApplication = applicationRepository.save(application);
            log.info("Application updated successfully: {}", updatedApplication.getCode());


            // ===== SYNC: Propagate changes to all related entities =====
            try {
                entitySyncService.syncApplicationChanges(oldApplication, updatedApplication);
                log.info("✅ Successfully synced application changes to related entities");
            } catch (Exception e) {
                log.error("Failed to sync application changes: {}", e.getMessage(), e);
                // Don't throw - the main update succeeded
            }

            // LOG HISTORY: Application update
            User currentUser = getCurrentUser();
            historyService.logApplicationUpdate(oldApplication, updatedApplication, currentUser);

            // Check if status changed
            if (!oldApplication.getStatus().equals(updatedApplication.getStatus())) {
                historyService.logApplicationStatusChange(updatedApplication,
                        oldApplication.getStatus(), updatedApplication.getStatus());
            }

            // ===== IF CHEF CHANGED, USE WORKLOAD SERVICE =====
            if (chefChanged && newChef != null && currentUser != null) {
                log.info("Chef de projet changed from {} to {} - using workload service",
                        oldChefId, newChefId);

                // Call workload service to handle assignment with validation and notification
                WorkloadService.AssignmentResult result = workloadService.assignApplication(
                        newChef.getId(),
                        updatedApplication.getId(),
                        false // force = false by default
                );

                if (result.isSuccess()) {
                    log.info("Workload validation passed for chef: {}", newChef.getUsername());
                } else if (result.isWarning()) {
                    log.warn("Workload warning for chef: {} - {}", newChef.getUsername(), result.getMessage());
                    // Still return success but with a warning message
                    return applicationMapper.toResponse(updatedApplication);
                } else if (result.isBlocked()) {
                    log.error("Workload blocked for chef: {} - {}", newChef.getUsername(), result.getMessage());
                    throw new RuntimeException("Cannot assign chef due to workload limits: " + result.getMessage());
                }
            }

            return applicationMapper.toResponse(updatedApplication);

        } catch (RuntimeException e) {
            log.error("Error updating application: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update application: " + e.getMessage());
        }
    }


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
            } else if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                // Chef de projet can only delete their own applications
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (application.getChefDeProjet() == null ||
                        !application.getChefDeProjet().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Access denied: You can only delete your own applications");
                }
            } else {
                // Other roles cannot delete applications
                throw new RuntimeException("Access denied: You don't have permission to delete applications");
            }

            // Check if application has conventions
            if (applicationRepository.hasConventions(id)) {
                throw new RuntimeException("Cannot delete application that has conventions. Delete conventions first.");
            }

            // LOG HISTORY: Application deletion (before deletion)
            User currentUser = getCurrentUser();
            historyService.logApplicationDelete(application, currentUser);

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


    @Transactional
    public void updateApplicationDatesFromConvention(Long applicationId, LocalDate conventionStartDate, LocalDate conventionEndDate) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        LocalDate oldStart = application.getDateDebut();
        LocalDate oldEnd = application.getDateFin();

        boolean datesChanged = false;

        // Update start date - ALWAYS follow the convention's start date
        // If convention start date is null, set application start date to null
        if (!Objects.equals(application.getDateDebut(), conventionStartDate)) {
            application.setDateDebut(conventionStartDate);
            datesChanged = true;
            log.info("Updated application {} start date to {} from convention",
                    application.getCode(), conventionStartDate);
        }

        // Update end date - ALWAYS follow the convention's end date
        // If convention end date is null, set application end date to null
        if (!Objects.equals(application.getDateFin(), conventionEndDate)) {
            application.setDateFin(conventionEndDate);
            datesChanged = true;
            log.info("Updated application {} end date to {} from convention",
                    application.getCode(), conventionEndDate);
        }

        if (datesChanged) {
            applicationRepository.save(application);
            log.info("Application {} dates updated successfully from convention", application.getCode());

            // LOG HISTORY: Dates sync
            historyService.logApplicationDatesSync(application, oldStart, oldEnd,
                    application.getDateDebut(), application.getDateFin());
        }
    }


    private Application cloneApplication(Application app) {
        Application clone = new Application();
        clone.setId(app.getId());
        clone.setCode(app.getCode());
        clone.setName(app.getName());
        clone.setDescription(app.getDescription());
        clone.setChefDeProjet(app.getChefDeProjet());
        clone.setClientName(app.getClientName());
        clone.setClientEmail(app.getClientEmail());
        clone.setClientPhone(app.getClientPhone());
        clone.setDateDebut(app.getDateDebut());
        clone.setDateFin(app.getDateFin());
        clone.setMinUser(app.getMinUser());
        clone.setMaxUser(app.getMaxUser());
        clone.setStatus(app.getStatus());
        clone.setCreatedAt(app.getCreatedAt());
        clone.setUpdatedAt(app.getUpdatedAt());
        return clone;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }


    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");
    }


    private User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username).orElse(null);
    }


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
            } else if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                // Chef de projet only sees their own applications
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                if (application.getChefDeProjet() == null ||
                        !application.getChefDeProjet().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("Access denied: You can only view your own applications");
                }
                return applicationMapper.toResponse(application);
            } else {
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


    @Transactional
    public ApplicationResponse manuallyTerminateApplication(Long id, String reason) {
        try {
            Application application = applicationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            String oldStatus = application.getStatus();

            // Calculate days remaining before termination
            Long daysRemaining = application.getDaysRemaining();
            User currentUser = getCurrentUser();

            // Manually set to TERMINE with tracking info
            application.setStatus("TERMINE");
            application.setTerminatedAt(LocalDateTime.now());
            application.setTerminatedBy(currentUser != null ?
                    currentUser.getUsername() : "UNKNOWN");
            application.setTerminationReason(reason != null ? reason : "Terminé manuellement");

            Application updatedApplication = applicationRepository.save(application);

            String daysText = daysRemaining != null ?
                    (daysRemaining > 0 ? " (" + daysRemaining + " jours avant échéance)" :
                            daysRemaining < 0 ? " (" + Math.abs(daysRemaining) + " jours après échéance)" :
                                    " (le jour de l'échéance)") : "";

            log.info("Application {} manually terminated by user{}. Terminated at: {}, Days remaining: {}",
                    application.getCode(), daysText, LocalDateTime.now(), daysRemaining);

            // LOG HISTORY: Manual status change
            historyService.logApplicationStatusChange(updatedApplication, oldStatus, "TERMINE");

            return applicationMapper.toResponse(updatedApplication);

        } catch (Exception e) {
            log.error("Error manually terminating application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to terminate application: " + e.getMessage());
        }
    }



    public List<ApplicationResponse> getAllApplications() {
        try {
            String currentUsername = getCurrentUsername();
            String currentRole = getCurrentUserRole();

            log.info("Fetching applications for user: {} with role: {}", currentUsername, currentRole);

            List<Application> applications;

            // ADMIN sees all NON-ARCHIVED applications
            if ("ROLE_ADMIN".equals(currentRole)) {
                applications = applicationRepository.findByArchivedFalse();
            }
            // CHEF_PROJET sees only their own NON-ARCHIVED applications
            else if ("ROLE_CHEF_PROJET".equals(currentRole)) {
                User currentUser = userRepository.findByUsername(currentUsername)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                applications = applicationRepository.findByChefDeProjetAndArchivedFalse(currentUser);
            }
            // DECIDEUR and COMMERCIAL_METIER see all NON-ARCHIVED applications
            else {
                applications = applicationRepository.findByArchivedFalse();
            }

            log.info("Returning {} non-archived applications to user {}", applications.size(), currentUsername);

            return applications.stream()
                    .map(applicationMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching applications: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch applications: " + e.getMessage());
        }
    }


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



    @Transactional
    public void calculateApplicationStatus(Long applicationId) throws RuntimeException {
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
            newStructure.setTypeStructure("Client");

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
        int expectedSequence = 1;

        for (int usedSequence : usedSequences) {
            if (usedSequence > expectedSequence) {
                // Found a gap!
                return String.format("APP-%d-%03d", currentYear, expectedSequence);
            }
            expectedSequence = usedSequence + 1;
        }

        // If we reached beyond 999, find first missing number
        if (expectedSequence > 999) {
            int missingSeq = findFirstMissingSequence(usedSequences);
            return String.format("APP-%d-%03d", currentYear, missingSeq);
        }

        // No gaps, use the next number
        return String.format("APP-%d-%03d", currentYear, expectedSequence);
    }

    private int findFirstMissingSequence(List<Integer> sequences) {
        Set<Integer> sequenceSet = new HashSet<>(sequences);

        for (int i = 1; i <= 999; i++) {
            if (!sequenceSet.contains(i)) {
                return i;
            }
        }

        return 1000; // Fallback
    }


    @Transactional
    public void syncApplicationDatesWithAllConventions(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Get all non-archived conventions for this application, sorted by updatedAt desc
        List<Convention> conventions = conventionRepository.findByApplicationAndArchivedFalseOrderByUpdatedAtDesc(application);

        if (conventions.isEmpty()) {
            log.info("No active conventions found for application {}", application.getCode());
            return;
        }

        // Get the most recent convention
        Convention mostRecentConvention = conventions.get(0);

        boolean datesChanged = false;

        // Follow the most recent convention's dates exactly
        if (!Objects.equals(application.getDateDebut(), mostRecentConvention.getDateDebut())) {
            application.setDateDebut(mostRecentConvention.getDateDebut());
            datesChanged = true;
            log.info("Synced application {} start date to most recent convention date: {}",
                    application.getCode(), mostRecentConvention.getDateDebut());
        }

        if (!Objects.equals(application.getDateFin(), mostRecentConvention.getDateFin())) {
            application.setDateFin(mostRecentConvention.getDateFin());
            datesChanged = true;
            log.info("Synced application {} end date to most recent convention date: {}",
                    application.getCode(), mostRecentConvention.getDateFin());
        }

        if (datesChanged) {
            applicationRepository.save(application);
            log.info("Application {} dates synced with most recent convention {}",
                    application.getCode(), mostRecentConvention.getReferenceConvention());
        }
    }


    public Map<String, Object> getApplicationDateSummary(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        List<Convention> conventions = conventionRepository.findByApplicationAndArchivedFalse(application);

        Map<String, Object> summary = new HashMap<>();
        summary.put("applicationId", applicationId);
        summary.put("applicationCode", application.getCode());
        summary.put("currentStartDate", application.getDateDebut());
        summary.put("currentEndDate", application.getDateFin());

        if (!conventions.isEmpty()) {
            LocalDate earliestStart = conventions.stream()
                    .map(Convention::getDateDebut)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            LocalDate latestEnd = conventions.stream()
                    .map(Convention::getDateFin)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            summary.put("conventionsCount", conventions.size());
            summary.put("earliestConventionStart", earliestStart);
            summary.put("latestConventionEnd", latestEnd);
            summary.put("isSynced",
                    Objects.equals(application.getDateDebut(), earliestStart) &&
                            Objects.equals(application.getDateFin(), latestEnd));
        } else {
            summary.put("conventionsCount", 0);
            summary.put("message", "No conventions found for this application");
        }

        return summary;
    }


    public List<ApplicationResponse> getApplicationsWithoutConventions() {
        try {
            List<Application> applications = applicationRepository.findApplicationsWithoutConventions();
            return applications.stream()
                    .map(applicationMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching applications without conventions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch applications without conventions");
        }
    }


    private boolean isAdmin() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        return currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));
    }

    public List<ApplicationResponse> getArchivedApplicationsForCurrentUser() throws RuntimeException {
        try {
            User currentUser = getCurrentUser();

            List<Application> archivedApps;

            if (isAdmin()) {
                // Admin sees all archived applications
                archivedApps = applicationRepository.findByArchivedTrue();
                log.info("Admin fetching all archived applications: found {}", archivedApps.size());
            } else {
                // Chef de projet sees only their own archived applications
                archivedApps = applicationRepository.findByChefDeProjetAndArchivedTrue(currentUser);
                log.info("Chef de projet {} fetching their archived applications: found {}",
                        currentUser.getUsername(), archivedApps.size());
            }

            return archivedApps.stream()
                    .map(applicationMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching archived applications: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch archived applications: " + e.getMessage());
        }
    }
}
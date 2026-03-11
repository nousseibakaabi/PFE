package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.ApplicationRequest;
import com.example.back.payload.response.ApplicationResponse;
import com.example.back.payload.response.MailResponse;
import com.example.back.repository.*;
import com.example.back.service.mapper.ApplicationMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private HistoryService historyService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private MailService mailService;

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

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
            }
            else if ("ROLE_CHEF_PROJET".equals(currentRole)) {
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
    public ApplicationResponse assignChefDeProjet(Long applicationId, Long chefDeProjetId) {
        try {
            // Check if current user is admin
            String currentRole = getCurrentUserRole();
            if (!"ROLE_ADMIN".equals(currentRole)) {
                throw new RuntimeException("Only admin can assign chef de projet");
            }

            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

            User oldChef = application.getChefDeProjet();

            User chefDeProjet = userRepository.findById(chefDeProjetId)
                    .orElseThrow(() -> new RuntimeException("Chef de Projet not found"));

            // Verify chef de projet has the correct role
            if (!chefDeProjet.getRoles().stream()
                    .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET"))) {
                throw new RuntimeException("Selected user is not a Chef de Projet");
            }

            // ===== USE WORKLOAD SERVICE TO HANDLE ASSIGNMENT =====
            User currentUser = getCurrentUser();

            WorkloadService.AssignmentResult result = workloadService.assignApplication(
                    chefDeProjetId,
                    applicationId,
                    false // force = false by default
            );

            if (!result.isSuccess() && !result.isWarning()) {
                throw new RuntimeException("Assignment failed: " + result.getMessage());
            }

            // LOG HISTORY: Assign chef de projet
            Application updatedApplication = applicationRepository.findById(applicationId).get();
            historyService.logApplicationAssignChef(updatedApplication, oldChef, chefDeProjet, currentUser);

            return applicationMapper.toResponse(updatedApplication);

        } catch (RuntimeException e) {
            log.error("Error assigning chef de projet: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error assigning chef de projet: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign chef de projet");
        }
    }


    private String formatAlternatives(List<WorkloadService.AlternativeChef> alternatives) {
        if (alternatives == null || alternatives.isEmpty()) {
            return "No available chefs found";
        }

        StringBuilder sb = new StringBuilder();
        for (WorkloadService.AlternativeChef alt : alternatives) {
            sb.append(String.format("• %s (Current workload: %.1f%%, Projected: %.1f%%)\n",
                    alt.getChefName(),
                    alt.getCurrentWorkload(),
                    alt.getProjectedWorkload()));
        }
        return sb.toString();
    }
    /**
     * Send assignment notification based on user's notification mode
     */
    private void sendAssignmentNotification(Application application, User chefDeProjet, User admin) {
        log.info("========== ASSIGNMENT NOTIFICATION ==========");
        log.info("Application: {} - {}", application.getCode(), application.getName());
        log.info("Chef de Projet: {} (ID: {})", chefDeProjet.getUsername(), chefDeProjet.getId());
        log.info("Chef Email: {}", chefDeProjet.getEmail());
        log.info("Chef Phone: {}", chefDeProjet.getPhone());
        log.info("Chef NotifMode: {}", chefDeProjet.getNotifMode());
        log.info("Admin: {} {}", admin.getFirstName(), admin.getLastName());

        String notifMode = chefDeProjet.getNotifMode();
        if (notifMode == null || notifMode.trim().isEmpty()) {
            notifMode = "email"; // Default to email
            log.info("NotifMode was null/empty, defaulting to: {}", notifMode);
        }

        String subject = "📋 Nouvelle application assignée: " + application.getCode();
        String message = buildAssignmentMessage(application, admin);

        boolean emailSent = false;
        boolean smsSent = false;

        // Send email if mode is email or both
        if (notifMode.equals("email") || notifMode.equals("both")) {
            log.info("Attempting to send EMAIL to: {}", chefDeProjet.getEmail());
            emailSent = sendAssignmentEmail(chefDeProjet, subject, message);
            if (emailSent) {
                log.info("✅ EMAIL sent successfully to {}", chefDeProjet.getEmail());
            } else {
                log.error("❌ Failed to send EMAIL to {}", chefDeProjet.getEmail());
            }
        }

        // Send SMS if mode is sms or both
        if (notifMode.equals("sms") || notifMode.equals("both")) {
            log.info("Attempting to send SMS to: {}", chefDeProjet.getPhone());
            smsSent = sendAssignmentSms(chefDeProjet, message);
            if (smsSent) {
                log.info("✅ SMS sent successfully to {}", chefDeProjet.getPhone());
            } else {
                log.error("❌ Failed to send SMS to {}", chefDeProjet.getPhone());
            }
        }

        log.info("Notification results - Email: {}, SMS: {}", emailSent, smsSent);
        log.info("========== END NOTIFICATION ==========");
    }


    private void sendAssignmentEmailViaNotification(User chefDeProjet, Notification notification, Facture dummyFacture) {
        try {
            // Use your existing mail service that works with notifications
            // You'll need to modify your MailService to handle assignment notifications
            // or use the notification service
            if (notificationService != null) {
                notificationService.sendNotificationViaChannels(notification, dummyFacture, 0);
                log.info("✅ Email notification sent via notification service to {}", chefDeProjet.getEmail());
            } else {
                log.error("NotificationService not available");
            }
        } catch (Exception e) {
            log.error("❌ Failed to send email notification: {}", e.getMessage());
        }
    }

    private void sendAssignmentSmsViaNotification(User chefDeProjet, Notification notification, Facture dummyFacture) {
        try {
            if (chefDeProjet.getPhone() == null || chefDeProjet.getPhone().isEmpty()) {
                log.warn("⚠️ User {} has no phone number, cannot send SMS", chefDeProjet.getUsername());
                return;
            }

            if (notificationService != null) {
                notificationService.sendNotificationViaChannels(notification, dummyFacture, 0);
                log.info("✅ SMS notification sent via notification service to {}", chefDeProjet.getPhone());
            } else {
                log.error("NotificationService not available");
            }
        } catch (Exception e) {
            log.error("❌ Failed to send SMS notification: {}", e.getMessage());
        }
    }
    /**
     * Send email notification
     */
    private boolean sendAssignmentEmail(User chefDeProjet, String subject, String message) {
        try {
            log.info("Preparing email for: {}", chefDeProjet.getEmail());

            // Create HTML email content
            String htmlContent = buildHtmlEmailContent(chefDeProjet, message);

            // Get system sender
            User systemSender = getSystemSender();

            com.example.back.payload.request.MailRequest request = new com.example.back.payload.request.MailRequest();
            request.setSubject(subject);
            request.setContent(htmlContent);
            request.setTo(java.util.Arrays.asList(chefDeProjet.getEmail()));
            request.setImportance("NORMAL");

            log.info("Calling mailService.sendMail()...");
            MailResponse response = mailService.sendMail(request, systemSender, null);

            log.info("Mail created with ID: {}", response.getId());
            return true;

        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            return false;
        }
    }
    /**
     * Send SMS notification
     */
    private boolean sendAssignmentSms(User chefDeProjet, String message) {
        try {
            // Check if user has phone number
            if (chefDeProjet.getPhone() == null || chefDeProjet.getPhone().isEmpty()) {
                log.warn("User {} has no phone number, skipping SMS", chefDeProjet.getUsername());
                return false;
            }

            // Check if SMS is enabled
            if (!smsService.isSmsEnabled()) {
                log.warn("SMS is disabled in configuration");
                return false;
            }

            // SMS should be shorter, so use a condensed version
            String smsMessage = buildSmsMessage(message);
            log.info("SMS message: {}", smsMessage);

            // Use the appropriate SMS method
            smsService.sendTestSms(chefDeProjet.getPhone(), smsMessage);

            log.info("SMS sent to {}", chefDeProjet.getPhone());
            return true;

        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Build the assignment message
     */
    private String buildAssignmentMessage(Application application, User admin) {
        return String.format(
                "Vous avez été assigné comme Chef de Projet pour l'application %s - %s par %s %s.\n\n" +
                        "Détails de l'application:\n" +
                        "• Code: %s\n" +
                        "• Nom: %s\n" +
                        "• Client: %s\n" +
                        "• Email client: %s\n" +
                        "• Téléphone client: %s\n" +
                        "• Dates: %s - %s\n" +
                        "• Statut: %s\n\n" +
                        "Connectez-vous à l'application pour plus de détails.",

                application.getCode(),
                application.getName(),
                admin.getFirstName(),
                admin.getLastName(),
                application.getCode(),
                application.getName(),
                application.getClientName(),
                application.getClientEmail() != null ? application.getClientEmail() : "Non spécifié",
                application.getClientPhone() != null ? application.getClientPhone() : "Non spécifié",
                application.getDateDebut() != null ? application.getDateDebut().toString() : "Non définie",
                application.getDateFin() != null ? application.getDateFin().toString() : "Non définie",
                application.getStatus()
        );
    }

    /**
     * Build HTML email content
     */
    private String buildHtmlEmailContent(User chefDeProjet, String message) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 20px; border-radius: 0 0 10px 10px; }" +
                ".footer { margin-top: 20px; font-size: 12px; color: #999; text-align: center; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h2>📋 Nouvelle assignation Chef de Projet</h2>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Bonjour <strong>" + chefDeProjet.getFirstName() + " " + chefDeProjet.getLastName() + "</strong>,</p>" +
                "<p>" + message.replace("\n", "<br>") + "</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Cet email a été envoyé automatiquement par le système de gestion.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    /**
     * Build SMS message (shorter version)
     */
    private String buildSmsMessage(String fullMessage) {
        // Extract just the essential info for SMS
        String[] lines = fullMessage.split("\n");
        StringBuilder sms = new StringBuilder();

        for (String line : lines) {
            if (line.contains("assigné comme Chef de Projet")) {
                sms.append(line).append(" ");
            } else if (line.contains("• Code:")) {
                sms.append(line.replace("• ", "")).append(" ");
            } else if (line.contains("• Nom:")) {
                sms.append(line.replace("• ", "")).append(" ");
            } else if (line.contains("• Client:")) {
                sms.append(line.replace("• ", "")).append(" ");
            } else if (line.contains("Connectez-vous")) {
                // Don't include login message in SMS to save space
                break;
            }
        }

        return sms.toString().trim();
    }

    /**
     * Get system sender user (you may need to create this user)
     */
    private User getSystemSender() {
        return userRepository.findByUsername("system")
                .orElseGet(() -> {
                    // If system user doesn't exist, return the admin as fallback
                    return userRepository.findByUsername("admin")
                            .orElseThrow(() -> new RuntimeException("No system or admin user found"));
                });
    }

    /**
     * Update application dates based on a convention
     * This is called when a convention is created or updated
     */
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

    /**
     * Clone application for history
     */
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

    /**
     * Get current user entity
     */
    private User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username).orElse(null);
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




    // In ApplicationService.java - Add this method

    /**
     * Manually set application status to TERMINE
     * This bypasses automatic date-based logic
     */
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


    /**
     * Generate suggested application code
     * Format: APP-YYYY-XXX where XXX is auto-incremented sequence
     */

    // ApplicationService.java
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






    /**
     * Sync all application dates based on its conventions
     * For multiple conventions, we need to decide which convention's dates to follow
     * Since an app can have multiple conventions, we need to define the logic:
     * - Should it follow the most recent convention?
     * - Should it take the earliest start and latest end?
     *
     * Based on your statement "the app will always follow the convention",
     * I'll assume you want it to follow the most recently created/updated convention
     */
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

    /**
     * Get date summary for an application based on its conventions
     */
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


    /**
     * Get all applications that don't have any conventions
     */
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

    private Notification createAssignmentNotification(Application application, User chefDeProjet, User admin) {
        try {
            Notification notification = new Notification();
            notification.setUser(chefDeProjet);
            notification.setTitle("📋 Nouvelle application assignée");
            notification.setType("SUCCESS");
            notification.setNotificationType("APPLICATION_ASSIGNED");

            String message = String.format(
                    "Vous avez été assigné comme Chef de Projet pour l'application %s - %s par %s %s.\n\n" +
                            "Détails de l'application:\n" +
                            "• Code: %s\n" +
                            "• Nom: %s\n" +
                            "• Client: %s\n" +
                            "• Dates: %s - %s\n" +
                            "• Statut: %s",
                    application.getCode(),
                    application.getName(),
                    admin.getFirstName(),
                    admin.getLastName(),
                    application.getCode(),
                    application.getName(),
                    application.getClientName(),
                    application.getDateDebut() != null ? application.getDateDebut().toString() : "Non définie",
                    application.getDateFin() != null ? application.getDateFin().toString() : "Non définie",
                    application.getStatus()
            );
            notification.setMessage(message);

            notification.setReferenceId(application.getId());
            notification.setReferenceType("APPLICATION");
            notification.setReferenceCode(application.getCode());

            notification.setIsRead(false);
            notification.setIsSent(false);
            notification.setEmailSent(false);
            notification.setSmsSent(false);

            // Save to database
            return notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to create assignment notification", e);
            return null;
        }
    }


// Add these methods to your ApplicationService class

    /**
     * Check if current user is admin
     */
    private boolean isAdmin() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        return currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));
    }

    /**
     * Get archived applications for current user (with role-based filtering)
     */
    public List<ApplicationResponse> getArchivedApplicationsForCurrentUser() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new RuntimeException("User not authenticated");
            }

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
    }}
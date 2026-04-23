package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.response.MailResponse;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.UserRepository;
import com.example.back.repository.WorkloadRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkloadService {

    private final WorkloadRepository workloadRepository;

    private final ApplicationRepository applicationRepository;

    private final UserRepository userRepository;

    private final MailService mailService;

    private final SmsService smsService;

    private final HistoryService historyService;

    // ============= THRESHOLDS =============
    private static final double BLOCK_THRESHOLD = 75.0;    // >80% = BLOCKED
    private static final double WARNING_THRESHOLD = 45.0;  // 70-80% = WARNING
    private static final double MEDIUM_THRESHOLD = 45.0;   // For UI display only

    public WorkloadService(WorkloadRepository workloadRepository, ApplicationRepository applicationRepository, UserRepository userRepository,MailService mailService, SmsService smsService, HistoryService historyService) {
        this.workloadRepository = workloadRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.smsService = smsService;
        this.historyService = historyService;
    }

    /**
     * Initialize or update workload for a chef - FIXED to count ALL applications
     */
    @Transactional
    public Workload initializeWorkload(Long chefId) {
        User chef = userRepository.findById(chefId)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        // Check if chef has the correct role
        boolean isChef = chef.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_CHEF_PROJET"));

        if (!isChef) {
            throw new RuntimeException("User is not a Chef de Projet");
        }

        Optional<Workload> existing = workloadRepository.findByChefDeProjet(chef);

        Workload workload;
        if (existing.isPresent()) {
            workload = existing.get();
            log.info("Updating existing workload for chef: {}", chef.getUsername());
        } else {
            workload = new Workload();
            workload.setChefDeProjet(chef);
            log.info("Creating new workload for chef: {}", chef.getUsername());
        }

        // Get ALL applications assigned to this chef (not just active)
        List<Application> assignedApps = applicationRepository.findByChefDeProjet(chef);

        log.info("Chef {} has {} total applications assigned", chef.getUsername(), assignedApps.size());

        // Count ALL applications regardless of status
        int totalApps = assignedApps.size();
        double totalValue = 0.0;
        long totalDuration = 0L;

        for (Application app : assignedApps) {
            // Calculate value from conventions
            double appValue = calculateApplicationValue(app);
            totalValue += appValue;

            // Calculate duration
            long appDuration = calculateApplicationDuration(app);
            totalDuration += appDuration;

            log.debug("App {}: value={}, duration={}", app.getCode(), appValue, appDuration);
        }

        workload.setCurrentApplicationsCount(totalApps);
        workload.setTotalApplicationsValue(totalValue);
        workload.setTotalApplicationsDuration(totalDuration);

        // Calculate workload percentage based on all three factors
        double workloadPercentage = calculateWorkloadPercentage(
                totalApps,
                totalValue,
                totalDuration
        );

        workload.setCurrentWorkloadScore(workloadPercentage);
        workload.setLastCalculatedAt(LocalDateTime.now());

        Workload saved = workloadRepository.save(workload);

        log.info("WORKLOAD UPDATED for {}: {}% ({} apps, {} TND, {} days)",
                chef.getUsername(),
                String.format("%.1f", saved.getCurrentWorkloadScore()),
                saved.getCurrentApplicationsCount(),
                String.format("%.2f", saved.getTotalApplicationsValue()),
                saved.getTotalApplicationsDuration());

        return saved;
    }


    private double calculateApplicationValue(Application app) {
        if (app.getConventions() == null || app.getConventions().isEmpty()) {
            return 0.0;
        }
        return app.getConventions().stream()
                .filter(conv -> !conv.getArchived())
                .mapToDouble(conv -> {
                    if (conv.getMontantTTC() != null) {
                        return conv.getMontantTTC().doubleValue();
                    }
                    return 0.0;
                })
                .sum();
    }

    /**
     * Calculate duration of an application in days
     */
    private Long calculateApplicationDuration(Application app) {
        if (app.getDateDebut() == null || app.getDateFin() == null) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(app.getDateDebut(), app.getDateFin());
    }


    // In WorkloadService.java - Replace checkAssignment and assignApplication methods

    /**
     * Check if chef can be assigned to an application
     */
    public AssignmentCheck checkAssignment(Long chefId, Long applicationId) {
        log.info("Checking if chef {} can be assigned to application {}", chefId, applicationId);

        // Ensure workload is up to date
        Workload workload = initializeWorkload(chefId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Calculate new app's value and duration
        double newAppValue = calculateApplicationValue(application);
        long newAppDuration = calculateApplicationDuration(application);

        // Projected values
        int projectedApps = workload.getCurrentApplicationsCount() + 1;
        double projectedValue = workload.getTotalApplicationsValue() + newAppValue;
        long projectedDuration = workload.getTotalApplicationsDuration() + newAppDuration;

        // Calculate projected workload percentage
        double projectedWorkload = calculateWorkloadPercentage(
                projectedApps,
                projectedValue,
                projectedDuration
        );

        AssignmentCheck check = new AssignmentCheck();
        check.setChefId(chefId);
        check.setChefName(workload.getChefDeProjet().getFirstName() + " " +
                workload.getChefDeProjet().getLastName());
        check.setApplicationId(applicationId);
        check.setApplicationName(application.getName());

        // Set analysis data
        WorkloadAnalysis analysis = new WorkloadAnalysis();
        analysis.setCurrentWorkload(workload.getCurrentWorkloadScore());
        analysis.setCurrentCount(workload.getCurrentApplicationsCount());
        analysis.setCurrentValue(workload.getTotalApplicationsValue());
        analysis.setCurrentDuration(workload.getTotalApplicationsDuration());
        analysis.setProjectedCount(projectedApps);
        analysis.setProjectedValue(projectedValue);
        analysis.setProjectedDuration(projectedDuration);
        analysis.setProjectedWorkload(projectedWorkload);

        check.setAnalysis(analysis);

        // ===== PERCENTAGE-BASED LOGIC =====
        // >75% = BLOCKED
        // 45-75% = OK (can assign)
        // <45% = DISPONIBLE (available)

        if (projectedWorkload > 75.0) {
            // >75% - BLOCKED
            check.setCanAssign(false);
            check.setAssignWithCaution(false);
            check.setStatus("BLOCKED");
            check.setMessage("Charge de travail critique (>75%) - Assignment bloqué sauf si forcé");
        }
        else if (projectedWorkload >= 45.0) {
            // 45-75% - OK (can assign)
            check.setCanAssign(true);
            check.setAssignWithCaution(false);
            check.setStatus("OK");
            check.setMessage("Charge de travail normale (" + String.format("%.1f", projectedWorkload) + "%) - Assignment possible");
        }
        else {
            // <45% - DISPONIBLE (available)
            check.setCanAssign(true);
            check.setAssignWithCaution(false);
            check.setStatus("DISPONIBLE");
            check.setMessage("Chef disponible - Charge de travail faible (" + String.format("%.1f", projectedWorkload) + "%)");
        }

        // Find alternative chefs (with better availability)
        List<Workload> allChefs = workloadRepository.findAll();
        List<AlternativeChef> alternatives = allChefs.stream()
                .filter(w -> !w.getChefDeProjet().getId().equals(chefId))
                .map(w -> {
                    int altProjectedApps = w.getCurrentApplicationsCount() + 1;
                    double altProjectedValue = w.getTotalApplicationsValue() + newAppValue;
                    long altProjectedDuration = w.getTotalApplicationsDuration() + newAppDuration;

                    double altProjectedWorkload = calculateWorkloadPercentage(
                            altProjectedApps,
                            altProjectedValue,
                            altProjectedDuration
                    );

                    AlternativeChef alt = new AlternativeChef();
                    alt.setChefId(w.getChefDeProjet().getId());
                    alt.setChefName(w.getChefDeProjet().getFirstName() + " " + w.getChefDeProjet().getLastName());
                    alt.setCurrentWorkload(w.getCurrentWorkloadScore());
                    alt.setProjectedWorkload(altProjectedWorkload);

                    // Alternative chef can accept if projected workload <= 75%
                    alt.setCanAccept(altProjectedWorkload <= 75.0);

                    // Calculate workload increase
                    alt.setWorkloadIncrease(altProjectedWorkload - w.getCurrentWorkloadScore());

                    return alt;
                })
                .filter(AlternativeChef::isCanAccept)
                .sorted((a1, a2) -> Double.compare(a1.getProjectedWorkload(), a2.getProjectedWorkload()))
                .limit(3)
                .collect(Collectors.toList());

        check.setAlternativeChefs(alternatives);

        return check;
    }

    /**
     * Helper method to calculate workload percentage
     */
    private double calculateWorkloadPercentage(int apps, double value, long duration) {
        // Weight factors
        double countWeight = 0.4;
        double valueWeight = 0.4;
        double durationWeight = 0.2;

        // Max limits
        int maxApps = 5;
        double maxValue = 5000000.0; // 5M TND
        long maxDuration = 730L; // 2 years

        // Calculate individual percentages (cap at 100%)
        double countPercentage = Math.min((apps * 100.0) / maxApps, 100);
        double valuePercentage = Math.min((value * 100.0) / maxValue, 100);
        double durationPercentage = Math.min((duration * 100.0) / maxDuration, 100);

        // Calculate weighted average
        return (countPercentage * countWeight) +
                (valuePercentage * valueWeight) +
                (durationPercentage * durationWeight);
    }

    /**
     * Assign application to chef with workload validation
     */
    @Transactional
    public AssignmentResult assignApplication(Long chefId, Long applicationId, boolean force) {
        log.info("========== ASSIGN APPLICATION CALLED ==========");
        log.info("Assigning application {} to chef {} (force: {})", applicationId, chefId, force);

        // First check if assignment is possible
        AssignmentCheck check = checkAssignment(chefId, applicationId);

        // ===== PERCENTAGE-BASED LOGIC WITH FORCE =====
        double projectedWorkload = check.getAnalysis().getProjectedWorkload();

        // If >75% AND not forced, return blocked
        if (projectedWorkload > 75.0 && !force) {
            log.warn("Assignment blocked: projected workload {}% > 75%", projectedWorkload);
            return AssignmentResult.blocked(check, "Assignment bloqué: Charge de travail >75%");
        }

        // If >75% but forced, proceed with warning
        if (projectedWorkload > 75.0 && force) {
            log.warn("FORCE ASSIGN: Proceeding despite workload being {}%", projectedWorkload);
        }

        // Proceed with assignment
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        User chef = userRepository.findById(chefId)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        User oldChef = application.getChefDeProjet();

        // Update application
        application.setChefDeProjet(chef);
        Application updatedApplication = applicationRepository.save(application);
        log.info("Application updated in database");

        // Recalculate workload
        Workload updatedWorkload = initializeWorkload(chefId);
        log.info("Workload recalculated: {}%", updatedWorkload.getCurrentWorkloadScore());

        // LOG HISTORY: Assign chef de projet
        try {
            User currentUser = getCurrentUser();
            if (historyService != null) {
                historyService.logApplicationAssignChef(updatedApplication, oldChef, chef, currentUser);
                log.info("History logged successfully");
            }
        } catch (Exception e) {
            log.error("Failed to log history: {}", e.getMessage());
        }

        // ===== SEND NOTIFICATION TO THE ASSIGNED CHEF DE PROJET =====
        try {
            User currentUser = getCurrentUser();
            log.info("Calling sendAssignmentNotification...");

            if (chef.getNotifMode() != null && !chef.getNotifMode().isEmpty()) {
                sendAssignmentNotification(updatedApplication, chef, currentUser);
                log.info("sendAssignmentNotification completed");
            } else {
                log.warn("Chef {} has no notification mode set, skipping notifications", chef.getUsername());
            }
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
        }

        // Prepare result based on what happened
        AssignmentResult result;
        if (projectedWorkload > 75.0 && force) {
            // Forced assignment (>75%)
            result = AssignmentResult.forced(check, "Application assignée (FORCÉE) malgré la charge critique (" +
                    String.format("%.1f", projectedWorkload) + "%)");
        } else if (projectedWorkload >= 45.0) {
            // Normal assignment (45-75%)
            result = AssignmentResult.success(check, "Application assignée avec succès. Charge: " +
                    String.format("%.1f", projectedWorkload) + "%");
        } else {
            // Available chef (<45%)
            result = AssignmentResult.success(check, "Application assignée à un chef disponible. Charge: " +
                    String.format("%.1f", projectedWorkload) + "%");
        }

        result.setUpdatedWorkload(updatedWorkload.getCurrentWorkloadScore());

        log.info("Application {} assigned to {}. New workload: {}%",
                application.getCode(),
                chef.getUsername(),
                String.format("%.1f", updatedWorkload.getCurrentWorkloadScore()));
        log.info("========== ASSIGN APPLICATION COMPLETED ==========");

        return result;
    }

    /**
     * Get workload dashboard
     */
    public WorkloadDashboard getWorkloadDashboard() {
        // Ensure all workloads are up to date
        List<User> allChefs = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getName().name().equals("ROLE_CHEF_PROJET")))
                .toList();

        for (User chef : allChefs) {
            initializeWorkload(chef.getId());
        }

        List<Workload> allWorkloads = workloadRepository.findAllOrderedByWorkload();

        WorkloadDashboard dashboard = new WorkloadDashboard();

        // Calculate statistics with new thresholds
        int totalChefs = allWorkloads.size();
        long overloadedChefs = allWorkloads.stream()
                .filter(w -> w.getCurrentWorkloadScore() > BLOCK_THRESHOLD)
                .count();
        long highWorkloadChefs = allWorkloads.stream()
                .filter(w -> w.getCurrentWorkloadScore() > WARNING_THRESHOLD &&
                        w.getCurrentWorkloadScore() <= BLOCK_THRESHOLD)
                .count();
        long availableChefs = allWorkloads.stream()
                .filter(w -> w.getCurrentWorkloadScore() <= WARNING_THRESHOLD)
                .count();

        dashboard.setTotalChefs(totalChefs);
        dashboard.setOverloadedChefs(overloadedChefs);
        dashboard.setHighWorkloadChefs(highWorkloadChefs);
        dashboard.setAvailableChefs(availableChefs);
        dashboard.setAverageWorkload(allWorkloads.stream()
                .mapToDouble(Workload::getCurrentWorkloadScore)
                .average()
                .orElse(0.0));

        // Create workload list
        dashboard.setWorkloads(allWorkloads.stream()
                .map(this::toWorkloadDTO)
                .collect(Collectors.toList()));

        return dashboard;
    }

    /**
     * Convert Workload to DTO
     */
    private WorkloadDTO toWorkloadDTO(Workload w) {
        WorkloadDTO dto = new WorkloadDTO();
        dto.setChefId(w.getChefDeProjet().getId());
        dto.setChefName(w.getChefDeProjet().getFirstName() + " " + w.getChefDeProjet().getLastName());
        dto.setCurrentWorkload(w.getCurrentWorkloadScore());
        dto.setCurrentApps(w.getCurrentApplicationsCount());
        dto.setTotalValue(w.getTotalApplicationsValue());
        dto.setTotalDuration(w.getTotalApplicationsDuration());

        // Determine status with new thresholds
        if (w.getCurrentWorkloadScore() > BLOCK_THRESHOLD) {
            dto.setStatus("CRITIQUE");
            dto.setStatusColor("red");
        } else if (w.getCurrentWorkloadScore() > MEDIUM_THRESHOLD) {
            dto.setStatus("MOYENNE");
            dto.setStatusColor("orange");
        } else {
            dto.setStatus("FAIBLE");
            dto.setStatusColor("green");
        }

        return dto;
    }

    // ============= NOTIFICATION METHODS =============

    /**
     * Send assignment notification based on user's notification mode
     */
    private void sendAssignmentNotification(Application application, User chefDeProjet, User admin) {
        log.info("========== NOTIFICATION DEBUG ==========");
        log.info("Starting notification process for chef: {}", chefDeProjet.getUsername());
        log.info("Chef ID: {}", chefDeProjet.getId());
        log.info("Chef Email: {}", chefDeProjet.getEmail());
        log.info("Chef Phone: {}", chefDeProjet.getPhone());
        log.info("Chef NotifMode: {}", chefDeProjet.getNotifMode());
        log.info("Application: {} - {}", application.getCode(), application.getName());
        log.info("Admin: {} {}", admin.getFirstName(), admin.getLastName());

        String notifMode = chefDeProjet.getNotifMode();
        if (notifMode == null || notifMode.trim().isEmpty()) {
            notifMode = "email"; // Default to email
        }

        String subject = "📋 Nouvelle application assignée: " + application.getCode();
        String message = buildAssignmentMessage(application, admin);

        // Send email if mode is email or both
        if (notifMode.equals("email") || notifMode.equals("both")) {
            log.info("Case: {} - sending email", notifMode);
            boolean emailSent = sendAssignmentEmail(chefDeProjet, subject, message);
            if (emailSent) {
                log.info("✅ Email sent successfully to {}", chefDeProjet.getEmail());
            } else {
                log.error("❌ Failed to send email to {}", chefDeProjet.getEmail());
            }
        }

        // Send SMS if mode is sms or both
        if (notifMode.equals("sms") || notifMode.equals("both")) {
            log.info("Case: {} - sending SMS", notifMode);
            boolean smsSent = sendAssignmentSms(chefDeProjet, message);
            if (smsSent) {
                log.info("✅ SMS sent successfully to {}", chefDeProjet.getPhone());
            } else {
                log.error("❌ Failed to send SMS to {}", chefDeProjet.getPhone());
            }
        }

        log.info("========== END NOTIFICATION DEBUG ==========");
    }

    private boolean sendAssignmentEmail(User chefDeProjet, String subject, String message) {
        log.info("----- EMAIL SENDING DEBUG -----");
        log.info("Preparing to send email to: {}", chefDeProjet.getEmail());

        try {
            // Create HTML email content
            String htmlContent = buildHtmlEmailContent(chefDeProjet, message);
            log.info("HTML content created, length: {} characters", htmlContent.length());

            // Get system sender
            log.info("Attempting to get system sender...");
            User systemSender = getSystemSender();
            log.info("System sender found: {} ({})", systemSender.getUsername(), systemSender.getEmail());

            com.example.back.payload.request.MailRequest request = new com.example.back.payload.request.MailRequest();
            request.setSubject(subject);
            request.setContent(htmlContent);
            request.setTo(java.util.Arrays.asList(chefDeProjet.getEmail()));
            request.setImportance("NORMAL");

            log.info("MailRequest created. Subject: {}, To: {}, Importance: {}",
                    request.getSubject(), request.getTo(), request.getImportance());

            log.info("Calling mailService.sendMail()...");
            MailResponse response = mailService.sendMail(request, systemSender, null);

            log.info("✅ Mail created in database with ID: {}", response.getId());
            log.info("✅ Assignment email successfully saved to database for {}", chefDeProjet.getEmail());
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to send assignment email to {}: {}",
                    chefDeProjet.getEmail(), e.getMessage());
            log.error("Exception details:", e);
            return false;
        }
    }

    private boolean sendAssignmentSms(User chefDeProjet, String message) {
        log.info("----- SMS SENDING DEBUG -----");

        try {
            // Check if user has phone number
            log.info("Chef phone number: '{}'", chefDeProjet.getPhone());

            if (chefDeProjet.getPhone() == null || chefDeProjet.getPhone().isEmpty()) {
                log.warn("⚠️ User {} has no phone number, cannot send SMS", chefDeProjet.getUsername());
                return false;
            }

            // Check if SMS is enabled
            if (!smsService.isSmsEnabled()) {
                log.warn("⚠️ SMS is disabled in configuration");
                return false;
            }

            // SMS should be shorter, so use a condensed version
            String smsMessage = buildSmsMessage(message);
            log.info("SMS message created: '{}'", smsMessage);
            log.info("SMS message length: {} characters", smsMessage.length());

            // Use sendDirectSms
            log.info("Calling smsService.sendDirectSms() to number: {}", chefDeProjet.getPhone());
            smsService.sendDirectSms(chefDeProjet.getPhone(), smsMessage);

            log.info("✅ Assignment SMS sent to {}", chefDeProjet.getPhone());
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to send assignment SMS to {}: {}",
                    chefDeProjet.getPhone(), e.getMessage());
            log.error("Exception details:", e);
            return false;
        }
    }

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
     * Get system sender user
     */
    private User getSystemSender() {
        return userRepository.findByUsername("system")
                .orElseGet(() -> {
                    // If system user doesn't exist, return the first admin as fallback
                    return userRepository.findAll().stream()
                            .filter(u -> u.getRoles().stream()
                                    .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("No system or admin user found"));
                });
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    /**
     * Get current user entity
     */
    private User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ============= INNER CLASSES =============

    public static class AssignmentCheck {
        private Long chefId;
        private String chefName;
        private Long applicationId;
        private String applicationName;
        private boolean canAssign;
        private boolean assignWithCaution;
        private String status;
        private String message;
        private WorkloadAnalysis analysis;
        private List<AlternativeChef> alternativeChefs;

        // Getters and setters
        public Long getChefId() { return chefId; }
        public void setChefId(Long chefId) { this.chefId = chefId; }
        public String getChefName() { return chefName; }
        public void setChefName(String chefName) { this.chefName = chefName; }
        public Long getApplicationId() { return applicationId; }
        public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
        public boolean isCanAssign() { return canAssign; }
        public void setCanAssign(boolean canAssign) { this.canAssign = canAssign; }
        public boolean isAssignWithCaution() { return assignWithCaution; }
        public void setAssignWithCaution(boolean assignWithCaution) { this.assignWithCaution = assignWithCaution; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public WorkloadAnalysis getAnalysis() { return analysis; }
        public void setAnalysis(WorkloadAnalysis analysis) { this.analysis = analysis; }
        public List<AlternativeChef> getAlternativeChefs() { return alternativeChefs; }
        public void setAlternativeChefs(List<AlternativeChef> alternativeChefs) { this.alternativeChefs = alternativeChefs; }
    }

    public static class WorkloadAnalysis {
        private Double currentWorkload;
        private Integer currentCount;
        private Double currentValue;
        private Long currentDuration;
        private Integer projectedCount;
        private Double projectedValue;
        private Long projectedDuration;
        private Double projectedWorkload;

        // Getters and setters
        public Double getCurrentWorkload() { return currentWorkload; }
        public void setCurrentWorkload(Double currentWorkload) { this.currentWorkload = currentWorkload; }
        public void setCurrentCount(Integer currentCount) { this.currentCount = currentCount; }
        public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
        public void setCurrentDuration(Long currentDuration) { this.currentDuration = currentDuration; }
        public void setProjectedCount(Integer projectedCount) { this.projectedCount = projectedCount; }
        public void setProjectedValue(Double projectedValue) { this.projectedValue = projectedValue; }
        public void setProjectedDuration(Long projectedDuration) { this.projectedDuration = projectedDuration; }
        public Double getProjectedWorkload() { return projectedWorkload; }
        public void setProjectedWorkload(Double projectedWorkload) { this.projectedWorkload = projectedWorkload; }
            }

    public static class AlternativeChef {
        private Long chefId;
        private String chefName;
        private Double currentWorkload;
        private Double projectedWorkload;
        private Double workloadIncrease;
        private boolean canAccept;

        // Getters and setters
        public void setChefId(Long chefId) { this.chefId = chefId; }
        public void setChefName(String chefName) { this.chefName = chefName; }
        public void setCurrentWorkload(Double currentWorkload) { this.currentWorkload = currentWorkload; }
        public Double getProjectedWorkload() { return projectedWorkload; }
        public void setProjectedWorkload(Double projectedWorkload) { this.projectedWorkload = projectedWorkload; }
        public void setWorkloadIncrease(Double workloadIncrease) { this.workloadIncrease = workloadIncrease; }
        public boolean isCanAccept() { return canAccept; }
        public void setCanAccept(boolean canAccept) { this.canAccept = canAccept; }
    }

    public static class AssignmentResult {
        private boolean success;
        private boolean warning;
        private boolean blocked;
        private boolean forced;
        private String message;
        private AssignmentCheck check;
        private Double updatedWorkload;

        private AssignmentResult(boolean success, boolean warning, boolean blocked, boolean forced,
                                 String message, AssignmentCheck check) {
            this.success = success;
            this.warning = warning;
            this.blocked = blocked;
            this.forced = forced;
            this.message = message;
            this.check = check;
        }

        public static AssignmentResult success(AssignmentCheck check, String message) {
            return new AssignmentResult(true, false, false, false, message, check);
        }

        public static AssignmentResult warning(AssignmentCheck check, String message) {
            return new AssignmentResult(true, true, false, false, message, check);
        }

        public static AssignmentResult blocked(AssignmentCheck check, String message) {
            return new AssignmentResult(false, false, true, false, message, check);
        }

        public static AssignmentResult forced(AssignmentCheck check, String message) {
            return new AssignmentResult(true, false, false, true, message, check);
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public boolean isWarning() { return warning; }
        public boolean isBlocked() { return blocked; }
        public boolean isForced() { return forced; }
        public String getMessage() { return message; }
        public AssignmentCheck getCheck() { return check; }
        public Double getUpdatedWorkload() { return updatedWorkload; }
        public void setUpdatedWorkload(Double updatedWorkload) { this.updatedWorkload = updatedWorkload; }
    }

    public static class WorkloadDashboard {
        private Integer totalChefs;
        private Long overloadedChefs;
        private Long highWorkloadChefs;
        private Long availableChefs;
        private Double averageWorkload;
        private List<WorkloadDTO> workloads;

        // Getters and setters
        public Integer getTotalChefs() { return totalChefs; }
        public void setTotalChefs(Integer totalChefs) { this.totalChefs = totalChefs; }
        public Long getOverloadedChefs() { return overloadedChefs; }
        public void setOverloadedChefs(Long overloadedChefs) { this.overloadedChefs = overloadedChefs; }
        public Long getHighWorkloadChefs() { return highWorkloadChefs; }
        public void setHighWorkloadChefs(Long highWorkloadChefs) { this.highWorkloadChefs = highWorkloadChefs; }
        public Long getAvailableChefs() { return availableChefs; }
        public void setAvailableChefs(Long availableChefs) { this.availableChefs = availableChefs; }
        public Double getAverageWorkload() { return averageWorkload; }
        public void setAverageWorkload(Double averageWorkload) { this.averageWorkload = averageWorkload; }
        public List<WorkloadDTO> getWorkloads() { return workloads; }
        public void setWorkloads(List<WorkloadDTO> workloads) { this.workloads = workloads; }
    }

    public static class WorkloadDTO {
        private Long chefId;
        private String chefName;
        private Double currentWorkload;
        private Integer currentApps;
        private Integer maxApps;
        private Double totalValue;
        private Double maxValue;
        private Long totalDuration;
        private Long maxDuration;
        private String status;
        private String statusColor;

        // Getters and setters
        public Long getChefId() { return chefId; }
        public void setChefId(Long chefId) { this.chefId = chefId; }
        public String getChefName() { return chefName; }
        public void setChefName(String chefName) { this.chefName = chefName; }
        public Double getCurrentWorkload() { return currentWorkload; }
        public void setCurrentWorkload(Double currentWorkload) { this.currentWorkload = currentWorkload; }
        public Integer getCurrentApps() { return currentApps; }
        public void setCurrentApps(Integer currentApps) { this.currentApps = currentApps; }
        public Integer getMaxApps() { return maxApps; }
        public void setMaxApps(Integer maxApps) { this.maxApps = maxApps; }
        public Double getTotalValue() { return totalValue; }
        public void setTotalValue(Double totalValue) { this.totalValue = totalValue; }
        public Double getMaxValue() { return maxValue; }
        public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }
        public Long getTotalDuration() { return totalDuration; }
        public void setTotalDuration(Long totalDuration) { this.totalDuration = totalDuration; }
        public Long getMaxDuration() { return maxDuration; }
        public void setMaxDuration(Long maxDuration) { this.maxDuration = maxDuration; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusColor() { return statusColor; }
        public void setStatusColor(String statusColor) { this.statusColor = statusColor; }
    }
}
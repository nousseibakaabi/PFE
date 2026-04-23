package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.RequestActionDTO;
import com.example.back.payload.response.MailResponse;
import com.example.back.payload.response.RequestResponse;
import com.example.back.repository.*;
import com.example.back.service.mapper.RequestMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RequestService {

    private final RequestRepository requestRepository;

    private final UserRepository userRepository;

    private final ApplicationRepository applicationRepository;

    private final WorkloadService workloadService;

    private final MailService mailService;

    private final RequestMapper requestMapper;

    private final HistoryService historyService;

    public RequestService(RequestRepository requestRepository, UserRepository userRepository, ApplicationRepository applicationRepository, WorkloadService workloadService, MailService mailService, RequestMapper requestMapper, HistoryService historyService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.workloadService = workloadService;
        this.mailService = mailService;
        this.requestMapper = requestMapper;
        this.historyService = historyService;
    }


    @Transactional
    public void sendRenewalNotificationToAdmin(Application application, Convention newConvention, User admin) {

        String subject = "🔄 Application renouvelée - " + application.getCode();

        String content = buildRenewalNotificationToAdminEmail(application, newConvention);

        // Find admin if not provided
        if (admin == null) {
            admin = getAdmin();
            log.info("Admin found: {}", admin != null ? admin.getUsername() : "NONE");
        }

        if (admin != null) {
            sendEmail(admin, subject, content, admin);
            log.info("✅ Renewal notification sent to admin {}", admin.getEmail());
        } else {
            log.error("❌ No admin found to send notification");
        }

        log.info("========== END SCENARIO 1 NOTIFICATION ==========");
    }


    @Transactional
    public Request createRenewalAcceptanceRequest(Convention convention, User chefDeProjet, User admin) {

        Request request = new Request();
        request.setRequestType("RENEWAL_ACCEPTANCE");
        request.setStatus("PENDING");
        request.setRequester(admin);
        request.setTargetUser(chefDeProjet);
        request.setApplication(convention.getApplication());
        request.setConvention(convention);
        request.setOldConvention(convention); // The convention being renewed
        request.setReason("La convention " + convention.getReferenceConvention() +
                " a été renouvelée. Voulez-vous continuer à travailler sur l'application " +
                convention.getApplication().getName() + " ?");

        Request savedRequest = requestRepository.save(request);

        // Send email notification to chef de projet
        sendRenewalAcceptanceRequestEmail(chefDeProjet, convention, savedRequest, admin);

        return savedRequest;
    }


    @Transactional
    public void sendReassignmentNotificationToChef(User chef, Application application, Convention convention, User admin) {

        String subject = "📋 Réassignation suite au renouvellement - " + application.getCode();

        String content = buildReassignmentEmail(chef, application, convention);

        sendEmail(chef, subject, content, admin);

        log.info("✅ Reassignment notification sent to chef {}", chef.getEmail());
        log.info("========== END SCENARIO 1.2 NOTIFICATION ==========");
    }


    @Transactional
    public void sendRenewalNotificationToChef(User chef, Application application, Convention newConvention) {

        String subject = "🔄 Votre application a été renouvelée - " + application.getCode();

        String content = buildRenewalNotificationToChefEmail(chef, application, newConvention);

        User admin = getAdmin();

        sendEmail(chef, subject, content, admin);

        log.info("✅ Renewal notification sent to chef {}", chef.getEmail());
        log.info("========== END SCENARIO 2 NOTIFICATION ==========");
    }


    @Transactional
    public Request createReassignmentSuggestion(Convention convention, User chefDeProjet,
                                                User recommendedChef, String reason, String recommendations) {

        // Get admin
        User admin = getAdmin();
        if (admin == null) {
            log.error("❌ No admin found to create request");
            throw new RuntimeException("No admin found");
        }

        Request request = new Request();
        request.setRequestType("REASSIGNMENT_SUGGESTION");
        request.setStatus("PENDING");
        request.setRequester(chefDeProjet);
        request.setTargetUser(admin);
        request.setApplication(convention.getApplication());
        request.setConvention(convention);
        request.setOldConvention(convention);
        request.setRecommendedChef(recommendedChef);
        request.setReason(reason);
        request.setRecommendations(recommendations);

        Request savedRequest = requestRepository.save(request);

        // Send email to admin
        sendReassignmentSuggestionToAdminEmail(chefDeProjet, recommendedChef, convention, savedRequest, admin);

        log.info("========== END SCENARIO 2.2 REQUEST CREATION ==========");
        return savedRequest;
    }


    @Transactional
    public RequestResponse processRequest(RequestActionDTO actionDTO, User processor) {

        Request request = requestRepository.findById(actionDTO.getRequestId())
                .orElseThrow(() -> {
                    log.error("❌ Request not found with ID: {}", actionDTO.getRequestId());
                    return new RuntimeException("Request not found");
                });

        log.info("Request found - Type: {}, Status: {}, Requester: {}, Target: {}",
                request.getRequestType(), request.getStatus(),
                request.getRequester() != null ? request.getRequester().getUsername() : "null",
                request.getTargetUser() != null ? request.getTargetUser().getUsername() : "null");

        // Route to the appropriate handler
        if ("RENEWAL_ACCEPTANCE".equals(request.getRequestType())) {
            log.info("Routing to RENEWAL_ACCEPTANCE handler");
            return handleRenewalAcceptance(request, actionDTO, processor);
        } else if ("REASSIGNMENT_SUGGESTION".equals(request.getRequestType())) {
            log.info("Routing to REASSIGNMENT_SUGGESTION handler");
            return handleReassignmentSuggestion(request, actionDTO, processor);
        }
else if ("REASSIGNMENT_REQUEST_FROM_CHEF".equals(request.getRequestType())) {
            log.info("Routing to REASSIGNMENT_REQUEST_FROM_CHEF handler");
            return handleReassignmentRequestFromChef(request, actionDTO, processor);
        }
else {
            log.error("❌ Unknown request type: {}", request.getRequestType());
            throw new RuntimeException("Unknown request type: " + request.getRequestType());
        }
    }


    @Transactional
    public RequestResponse handleReassignmentRequestFromChef(Request request, RequestActionDTO actionDTO, User processor) {

        // Verify that the processor is admin
        if (!isAdmin(processor)) {
            log.error("❌ Access denied: Only admin can process reassignment requests");
            throw new RuntimeException("Only admin can process reassignment requests");
        }

        if ("APPROVE".equals(actionDTO.getAction())) {
            log.info("Admin APPROVING the reassignment request");

            User recommendedChef ;

            // If admin selected a different chef in the action, use that
            if (actionDTO.getRecommendedChefId() != null) {
                recommendedChef = userRepository.findById(actionDTO.getRecommendedChefId())
                        .orElseThrow(() -> new RuntimeException("Recommended chef not found"));

            }
            // Otherwise use the chef originally recommended in the request
            else if (request.getRecommendedChef() != null) {
                recommendedChef = request.getRecommendedChef();

            } else {
                throw new RuntimeException("No recommended chef specified");
            }

            // Check workload of recommended chef
            boolean canAccept = checkChefWorkload(recommendedChef, request.getApplication());

            if (!canAccept) {
                throw new RuntimeException("Le chef recommandé a une charge de travail trop élevée");
            }

            // Update application with new chef
            Application app = request.getApplication();
            User oldChef = app.getChefDeProjet();


            app.setChefDeProjet(recommendedChef);
            applicationRepository.save(app);

            historyService.logChefReassignment(app, oldChef, recommendedChef, processor,
                    request.getReason() + (request.getRecommendations() != null ? " - " + request.getRecommendations() : ""));

// LOG HISTORY: Request processed
            historyService.logReassignmentRequestProcessed(request, processor, "APPROVE", null);

            request.setStatus("APPROVED");
            request.setRecommendedChef(recommendedChef);
            request.setProcessedAt(LocalDateTime.now());

            // Send emails: one to old chef (requester) and one to new chef
            assert oldChef != null;
            sendReassignmentRequestApprovedEmails(request, recommendedChef, oldChef, processor);

        } else if ("DENY".equals(actionDTO.getAction())) {
            log.info("Admin DENYING the reassignment request");

            if (actionDTO.getReason() == null || actionDTO.getReason().trim().isEmpty()) {
                throw new RuntimeException("Reason is required when denying");
            }

            request.setStatus("DENIED");
            request.setDenialReason(actionDTO.getReason());
            request.setProcessedAt(LocalDateTime.now());

            historyService.logReassignmentRequestProcessed(request, processor, "DENY", actionDTO.getReason());

            // Send denial email to the requesting chef
            sendReassignmentRequestDeniedEmail(request, actionDTO.getReason(), processor);

        } else {
            throw new RuntimeException("Invalid action: " + actionDTO.getAction());
        }

        Request updatedRequest = requestRepository.save(request);
        log.info("========== END REASSIGNMENT REQUEST HANDLING ==========");

        return requestMapper.toResponse(updatedRequest);
    }

    /**
     * Send emails when reassignment request is approved
     */
    private void sendReassignmentRequestApprovedEmails(Request request, User newChef, User oldChef, User admin) {
        // Email to old chef (requester) -告诉他们请求已批准
        String subjectToOld = "✅ Demande de réassignation approuvée - " + request.getApplication().getCode();

        String contentToOld = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>✅ Demande approuvée</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + oldChef.getFirstName() + " " + oldChef.getLastName() + "</strong>,</p>" +
                "<p>Votre demande de réassignation pour l'application <strong>" + request.getApplication().getName() + "</strong> a été approuvée.</p>" +
                "<p><strong>" + newChef.getFirstName() + " " + newChef.getLastName() + "</strong> a été assigné comme nouveau chef de projet.</p>" +
                "<p>Vous n'êtes plus responsable de cette application.</p>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(oldChef, subjectToOld, contentToOld, admin);
        log.info("✅ Approval email sent to requesting chef {}", oldChef.getEmail());

        // Email to new chef -告诉他们被分配了
        String subjectToNew = "📋 Nouvelle assignation - " + request.getApplication().getCode();

        String contentToNew = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>📋 Nouvelle assignation</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + newChef.getFirstName() + " " + newChef.getLastName() + "</strong>,</p>" +
                "<p>Vous avez été assigné comme Chef de Projet pour l'application <strong>" +
                request.getApplication().getName() + "</strong>.</p>" +
                "<p>Cette assignation fait suite à une demande de réassignation du chef de projet précédent.</p>";

        if (request.getRecommendations() != null && !request.getRecommendations().isEmpty()) {
            contentToNew += "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                    "<p><strong>Recommandation :</strong> " + request.getRecommendations() + "</p>" +
                    "</div>";
        }

        contentToNew += "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/applications/" + request.getApplication().getId() + "' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir l'application</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(newChef, subjectToNew, contentToNew, admin);
        log.info("✅ Assignment email sent to new chef {}", newChef.getEmail());
    }


    private void sendReassignmentRequestDeniedEmail(Request request, String reason, User admin) {
        User chef = request.getRequester();

        String subject = "❌ Demande de réassignation refusée - " + request.getApplication().getCode();

        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>❌ Demande refusée</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong>,</p>" +
                "<p>Votre demande de réassignation pour l'application <strong>" + request.getApplication().getName() + "</strong> a été refusée.</p>" +
                "<div style='background: #fee; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='margin-top: 0; color: #dc2626;'>Raison du refus :</h3>" +
                "<p>" + reason + "</p>" +
                "</div>" +
                "<p>Vous devez continuer à travailler sur cette application.</p>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(chef, subject, content, admin);
    }


    @Transactional
    public RequestResponse handleRenewalAcceptance(Request request, RequestActionDTO actionDTO, User processor) {

        // Verify that the processor is the target user (the chef who should respond)
        if (request.getTargetUser() == null || !request.getTargetUser().getId().equals(processor.getId())) {
            log.error("❌ Access denied: Processor {} is not the target user {}",
                    processor.getId(),
                    request.getTargetUser() != null ? request.getTargetUser().getId() : "null");
            throw new RuntimeException("You can only respond to your own requests");
        }

        if ("APPROVE".equals(actionDTO.getAction())) {
            log.info("Chef APPROVING the request");

            // Chef accepts - mark request as approved
            request.setStatus("APPROVED");
            request.setProcessedAt(LocalDateTime.now());

            // Send confirmation email to chef
            sendRenewalApprovedEmail(processor, request);

            log.info("✅ Chef {} approved renewal acceptance", processor.getUsername());

        } else if ("DENY".equals(actionDTO.getAction())) {
            log.info("Chef DENYING the request");

            // Chef denies - need reason
            if (actionDTO.getReason() == null || actionDTO.getReason().trim().isEmpty()) {
                throw new RuntimeException("Reason is required when denying");
            }

            request.setStatus("DENIED");
            request.setDenialReason(actionDTO.getReason());
            request.setRecommendations(actionDTO.getRecommendations());

            if (actionDTO.getRecommendedChefId() != null) {
                User recommendedChef = userRepository.findById(actionDTO.getRecommendedChefId()).orElse(null);
                request.setRecommendedChef(recommendedChef);
                log.info("Recommended chef: {}", recommendedChef != null ? recommendedChef.getUsername() : "null");
            }

            request.setProcessedAt(LocalDateTime.now());

            // Send email to admin with reason
            sendRenewalDeniedEmailToAdmin(processor, request, actionDTO.getReason());

            log.info("✅ Chef {} denied renewal acceptance. Reason: {}", processor.getUsername(), actionDTO.getReason());
        } else {
            throw new RuntimeException("Invalid action: " + actionDTO.getAction());
        }

        Request updatedRequest = requestRepository.save(request);
        log.info("========== END RENEWAL ACCEPTANCE HANDLING ==========");

        return requestMapper.toResponse(updatedRequest);
    }


    @Transactional
    public RequestResponse handleReassignmentSuggestion(Request request, RequestActionDTO actionDTO, User processor) {
        log.info("========== HANDLING REASSIGNMENT SUGGESTION ==========");
        log.info("Request ID: {}", request.getId());
        log.info("Processor: {} (ID: {})", processor.getUsername(), processor.getId());
        log.info("Is admin: {}", isAdmin(processor));

        // Verify that the processor is admin
        if (!isAdmin(processor)) {
            log.error("❌ Access denied: Only admin can process reassignment suggestions");
            throw new RuntimeException("Only admin can process reassignment suggestions");
        }

        if ("APPROVE".equals(actionDTO.getAction())) {
            log.info("Admin APPROVING the suggestion");

            // Determine which chef to assign
            User recommendedChef ;

            // If admin selected a different chef in the action, use that
            if (actionDTO.getRecommendedChefId() != null) {
                recommendedChef = userRepository.findById(actionDTO.getRecommendedChefId())
                        .orElseThrow(() -> new RuntimeException("Recommended chef not found"));
                log.info("Admin selected recommended chef: {} (ID: {})",
                        recommendedChef.getUsername(), recommendedChef.getId());
            }
            // Otherwise use the chef originally recommended in the request
            else if (request.getRecommendedChef() != null) {
                recommendedChef = request.getRecommendedChef();
                log.info("Using originally recommended chef: {} (ID: {})",
                        recommendedChef.getUsername(), recommendedChef.getId());
            } else {
                log.error("❌ No recommended chef specified");
                throw new RuntimeException("No recommended chef specified");
            }

            // Check workload of recommended chef
            log.info("Checking workload for recommended chef {}...", recommendedChef.getUsername());
            boolean canAccept = checkChefWorkload(recommendedChef, request.getApplication());
            log.info("Workload check result: canAccept = {}", canAccept);

            if (!canAccept) {
                log.error("❌ Recommended chef {} has too high workload", recommendedChef.getUsername());
                throw new RuntimeException("Le chef recommandé a une charge de travail trop élevée");
            }

            // Update application with new chef
            Application app = request.getApplication();
            User oldChef = app.getChefDeProjet();

            log.info("Updating application {} - current chef: {}, new chef: {}",
                    app.getCode(),
                    oldChef != null ? oldChef.getUsername() : "none",
                    recommendedChef.getUsername());

            app.setChefDeProjet(recommendedChef);
            applicationRepository.save(app);
            log.info("✅ Application updated with new chef");


            request.setStatus("APPROVED");
            request.setRecommendedChef(recommendedChef);
            request.setProcessedAt(LocalDateTime.now());

            // Send emails to both chefs
            assert oldChef != null;
            sendReassignmentApprovedEmails(request, recommendedChef, oldChef, processor);
            log.info("✅ Approval emails sent");

        } else if ("DENY".equals(actionDTO.getAction())) {
            log.info("Admin DENYING the suggestion");

            if (actionDTO.getReason() == null || actionDTO.getReason().trim().isEmpty()) {
                log.error("❌ Denial reason is required but was empty");
                throw new RuntimeException("Reason is required when denying");
            }

            request.setStatus("DENIED");
            request.setDenialReason(actionDTO.getReason());
            request.setProcessedAt(LocalDateTime.now());

            // Send denial email to chef
            sendReassignmentDeniedEmail(request, actionDTO.getReason(), processor);
            log.info("✅ Denial email sent to chef");

        } else {
            log.error("❌ Invalid action: {}", actionDTO.getAction());
            throw new RuntimeException("Invalid action: " + actionDTO.getAction());
        }

        Request updatedRequest = requestRepository.save(request);
        log.info("✅ Request updated with status: {}", updatedRequest.getStatus());
        log.info("========== END REASSIGNMENT SUGGESTION HANDLING ==========");

        return requestMapper.toResponse(updatedRequest);
    }


    private boolean checkChefWorkload(User chef, Application application) {
        try {
            log.info("Checking workload for chef {} (ID: {}) on application {} (ID: {})",
                    chef.getUsername(), chef.getId(),
                    application.getCode(), application.getId());

            // Use the existing WorkloadService to check assignment
            WorkloadService.AssignmentCheck check = workloadService.checkAssignment(chef.getId(), application.getId());

            if (check == null || check.getAnalysis() == null) {
                log.warn("Could not get workload analysis for chef {}, defaulting to true", chef.getId());
                return true;
            }

            double projectedWorkload = check.getAnalysis().getProjectedWorkload();
            String status = check.getStatus();

            log.info("Chef {} workload check - Current: {}%, Projected: {}%, Status: {}, CanAssign: {}",
                    chef.getUsername(),
                    check.getAnalysis().getCurrentWorkload() != null ?
                            String.format("%.1f", check.getAnalysis().getCurrentWorkload()) : "N/A",
                    String.format("%.1f", projectedWorkload),
                    status,
                    check.isCanAssign());

            // Chef can accept if they are not BLOCKED (<=75%)
            boolean canAccept = !"BLOCKED".equals(status) && check.isCanAssign();

            log.info("Chef {} can accept: {}", chef.getUsername(), canAccept);

            return canAccept;

        } catch (Exception e) {
            log.error("Error checking workload for chef {}: {}", chef.getId(), e.getMessage(), e);
            // In case of error, allow the assignment (better to let admin decide)
            return true;
        }
    }


    public List<RequestResponse> getUserRequests(User user) {
        log.info("========== FETCHING REQUESTS FOR USER ==========");
        log.info("User: {} (ID: {})", user.getUsername(), user.getId());

        List<Request> requests = requestRepository.findUserRequests(user);

        log.info("Found {} requests in database", requests.size());

        for (Request req : requests) {
            log.info("Request ID: {}, Type: {}, Status: {}, Requester: {}, Target: {}",
                    req.getId(), req.getRequestType(), req.getStatus(),
                    req.getRequester() != null ? req.getRequester().getUsername() : "null",
                    req.getTargetUser() != null ? req.getTargetUser().getUsername() : "null");
        }

        List<RequestResponse> responses = requests.stream()
                .map(requestMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Returning {} request responses", responses.size());
        log.info("========== END FETCHING REQUESTS ==========");

        return responses;
    }


    public List<RequestResponse> getRequestsByStatus(String status) {
        log.info("Fetching requests with status: {}", status);
        List<Request> requests = requestRepository.findByStatus(status);
        log.info("Found {} requests with status {}", requests.size(), status);

        return requests.stream()
                .map(requestMapper::toResponse)
                .collect(Collectors.toList());
    }


    private String buildRenewalNotificationToAdminEmail(Application application, Convention convention) {
        User currentChef = application.getChefDeProjet();

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>🔄 Application renouvelée</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour Administrateur,</p>" +
                "<p>L'application <strong>" + application.getCode() + " - " + application.getName() + "</strong> a été renouvelée.</p>" +
                "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p><strong>Chef de projet actuel :</strong> " +
                (currentChef != null ?
                        currentChef.getFirstName() + " " + currentChef.getLastName() + " (" + currentChef.getUsername() + ")" :
                        "Non assigné") + "</p>" +
                "<p><strong>Nouvelle convention :</strong> " + convention.getReferenceConvention() + "</p>" +
                "<p><strong>Application créée par :</strong> " +
                (application.getCreatedBy() != null ?
                        application.getCreatedBy().getFirstName() + " " + application.getCreatedBy().getLastName() :
                        "Inconnu") + "</p>" +
                "</div>" +
                "<p>Veuillez vérifier la charge de travail du chef de projet.</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/applications/" + application.getId() + "' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir l'application</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildRenewalNotificationToChefEmail(User chef, Application application, Convention convention) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>🔄 Votre application a été renouvelée</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong>,</p>" +
                "<p>Votre application <strong>" + application.getCode() + " - " + application.getName() + "</strong> a été renouvelée.</p>" +
                "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p><strong>Nouvelle convention :</strong> " + convention.getReferenceConvention() + "</p>" +
                "</div>" +
                "<p>Si vous ne pouvez pas continuer à travailler sur cette application en raison de votre charge de travail, veuillez soumettre une demande de réassignation dans l'onglet 'Demandes'.</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/requests' style='background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin-right: 10px;'>Voir mes demandes</a>" +
                "<a href='http://localhost:4200/applications/" + application.getId() + "' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir l'application</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private void sendRenewalAcceptanceRequestEmail(User chef, Convention convention, Request request, User admin) {
        String subject = "📋 Demande de confirmation - Renouvellement d'application";

        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>📋 Demande de confirmation</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong>,</p>" +
                "<p>L'application <strong>" + convention.getApplication().getCode() + " - " +
                convention.getApplication().getName() + "</strong> a été renouvelée.</p>" +
                "<p>L'administrateur souhaite savoir si vous pouvez continuer à travailler sur cette application.</p>" +
                "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p><strong>Message :</strong> " + request.getReason() + "</p>" +
                "</div>" +
                "<p>Veuillez répondre à cette demande dans l'onglet 'Demandes'.</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/requests' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir mes demandes</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(chef, subject, content, admin);
        log.info("✅ Renewal acceptance request email sent to chef {}", chef.getEmail());
    }

    private void sendRenewalApprovedEmail(User chef, Request request) {
        User admin = getAdmin();

        String subject = "✅ Demande approuvée - " + request.getApplication().getCode();

        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>✅ Demande approuvée</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong>,</p>" +
                "<p>Votre demande pour l'application <strong>" + request.getApplication().getName() + "</strong> a été approuvée.</p>" +
                "<p>Vous êtes réassigné à cette application suite au renouvellement.</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/applications/" + request.getApplication().getId() + "' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir l'application</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(chef, subject, content, admin);
        log.info("✅ Renewal approved email sent to chef {}", chef.getEmail());
    }

    private void sendRenewalDeniedEmailToAdmin(User chef, Request request, String reason) {
        User admin = getAdmin();

        String subject = "❌ Demande refusée - " + request.getApplication().getCode();

        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>❌ Demande refusée</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour Administrateur,</p>" +
                "<p>Le chef de projet <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong> a refusé de continuer à travailler sur l'application <strong>" +
                request.getApplication().getName() + "</strong>.</p>" +
                "<div style='background: #fee; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='margin-top: 0; color: #dc2626;'>Raison du refus :</h3>" +
                "<p>" + reason + "</p>" +
                "</div>";

        if (request.getRecommendedChef() != null) {
            content += "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                    "<h3 style='margin-top: 0; color: #333;'>Chef recommandé :</h3>" +
                    "<p><strong>" + request.getRecommendedChef().getFirstName() + " " + request.getRecommendedChef().getLastName() + "</strong></p>" +
                    "<p><em>" + (request.getRecommendations() != null ? request.getRecommendations() : "") + "</em></p>" +
                    "</div>";
        }

        content += "<p>Veuillez assigner un autre chef de projet.</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/applications/" + request.getApplication().getId() + "' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Assigner un chef</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(admin, subject, content, admin);
        log.info("✅ Renewal denied email sent to admin");
    }

    private String buildReassignmentEmail(User chef, Application application, Convention convention) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>📋 Réassignation suite au renouvellement</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong>,</p>" +
                "<p>Vous avez été réassigné comme Chef de Projet pour l'application <strong>" +
                application.getCode() + " - " + application.getName() + "</strong> suite à son renouvellement.</p>" +
                "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<p><strong>Nouvelle convention :</strong> " + convention.getReferenceConvention() + "</p>" +
                "</div>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/applications/" + application.getId() + "' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir l'application</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private void sendReassignmentSuggestionToAdminEmail(User chef, User recommendedChef, Convention convention,
                                                        Request request, User admin) {
        String subject = "💡 Suggestion de réassignation - " + convention.getApplication().getCode();

        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>💡 Suggestion de réassignation</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour Administrateur,</p>" +
                "<p>Le chef de projet <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong> ne peut pas continuer à travailler sur l'application <strong>" +
                convention.getApplication().getName() + "</strong>.</p>";

        if (recommendedChef != null) {
            content += "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                    "<h3 style='margin-top: 0; color: #333;'>Chef recommandé :</h3>" +
                    "<p><strong>" + recommendedChef.getFirstName() + " " + recommendedChef.getLastName() + "</strong></p>" +
                    "</div>";
        }

        content += "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='margin-top: 0; color: #333;'>Raison :</h3>" +
                "<p>" + request.getReason() + "</p>" +
                "</div>";

        if (request.getRecommendations() != null && !request.getRecommendations().isEmpty()) {
            content += "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                    "<h3 style='margin-top: 0; color: #333;'>Recommandations :</h3>" +
                    "<p>" + request.getRecommendations() + "</p>" +
                    "</div>";
        }

        content += "<p>Veuillez examiner cette suggestion dans l'onglet 'Demandes'.</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/requests' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir les demandes</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(admin, subject, content, admin);
        log.info("✅ Reassignment suggestion email sent to admin");
    }

    private void sendReassignmentApprovedEmails(Request request, User newChef, User oldChef, User admin) {
        // Email to old chef
        String subjectToOld = "✅ Suggestion approuvée - " + request.getApplication().getCode();

        String contentToOld = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>✅ Suggestion approuvée</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + oldChef.getFirstName() + " " + oldChef.getLastName() + "</strong>,</p>" +
                "<p>Votre suggestion de réassignation pour l'application <strong>" + request.getApplication().getName() + "</strong> a été approuvée.</p>" +
                "<p><strong>" + newChef.getFirstName() + " " + newChef.getLastName() + "</strong> a été assigné comme nouveau chef de projet.</p>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(oldChef, subjectToOld, contentToOld, admin);
        log.info("✅ Approval email sent to old chef {}", oldChef.getEmail());

        // Email to new chef
        String subjectToNew = "📋 Nouvelle assignation suite à renouvellement - " + request.getApplication().getCode();

        String contentToNew = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>📋 Nouvelle assignation</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + newChef.getFirstName() + " " + newChef.getLastName() + "</strong>,</p>" +
                "<p>Vous avez été assigné comme Chef de Projet pour l'application <strong>" +
                request.getApplication().getName() + "</strong> suite au renouvellement de sa convention.</p>";

        if (request.getRecommendations() != null && !request.getRecommendations().isEmpty()) {
            contentToNew += "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                    "<p><strong>Recommandation :</strong> " + request.getRecommendations() + "</p>" +
                    "</div>";
        }

        contentToNew += "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/applications/" + request.getApplication().getId() + "' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir l'application</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(newChef, subjectToNew, contentToNew, admin);
        log.info("✅ Approval email sent to new chef {}", newChef.getEmail());
    }

    private void sendReassignmentDeniedEmail(Request request, String reason, User admin) {
        User chef = request.getRequester();

        String subject = "❌ Suggestion refusée - " + request.getApplication().getCode();

        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>❌ Suggestion refusée</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong>,</p>" +
                "<p>Votre suggestion de réassignation pour l'application <strong>" + request.getApplication().getName() + "</strong> a été refusée.</p>" +
                "<div style='background: #fee; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='margin-top: 0; color: #dc2626;'>Raison du refus :</h3>" +
                "<p>" + reason + "</p>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(chef, subject, content, admin);
        log.info("✅ Denial email sent to chef {}", chef.getEmail());
    }

    private void sendEmail(User recipient, String subject, String content, User sender) {
        try {
            if (sender == null) {
                sender = getAdmin();
                log.info("Sender was null, using admin: {}", sender != null ? sender.getUsername() : "null");
            }

            if (recipient == null) {
                log.error("❌ Cannot send email: recipient is null");
                return;
            }

            log.info("Sending email to: {} ({})", recipient.getEmail(), recipient.getUsername());
            log.info("Subject: {}", subject);

            com.example.back.payload.request.MailRequest request = new com.example.back.payload.request.MailRequest();
            request.setSubject(subject);
            request.setContent(content);
            request.setTo(List.of(recipient.getEmail()));
            request.setImportance("NORMAL");

            assert sender != null;
            MailResponse response = mailService.sendMail(request, sender, null);

            log.info("✅ Email sent successfully. Mail ID: {}", response.getId());

        } catch (Exception e) {
            assert recipient != null;
            log.error("❌ Failed to send email to {}: {}", recipient.getEmail(), e.getMessage(), e);
        }
    }

    private User getAdmin() {
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")))
                .findFirst()
                .orElse(null);

        log.info("getAdmin() returned: {}", admin != null ? admin.getUsername() : "null");
        return admin;
    }

    private boolean isAdmin(User user) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
        log.info("isAdmin check for {}: {}", user.getUsername(), isAdmin);
        return isAdmin;
    }

    /**
     * Create a reassignment request from a chef (for apps they created)
     * Scenario: Chef created the app but due to workload wants to reassign
     */
    @Transactional
    public Request createReassignmentRequestFromChef(Application application,
                                                     Convention convention,
                                                     User chefDeProjet,
                                                     User recommendedChef,
                                                     String reason,
                                                     String recommendations) {
        log.info("========== CREATING REASSIGNMENT REQUEST FROM CHEF ==========");
        log.info("Chef de projet (requester): {} (ID: {})", chefDeProjet.getUsername(), chefDeProjet.getId());
        log.info("Application: {} - {}", application.getCode(), application.getName());
        log.info("Application created by: {}", application.getCreatedBy() != null ?
                application.getCreatedBy().getUsername() : "UNKNOWN");
        log.info("Recommended chef: {} (ID: {})", recommendedChef.getUsername(), recommendedChef.getId());
        log.info("Reason: {}", reason);
        log.info("Recommendations: {}", recommendations);

        // Verify that the chef is actually the creator
        if (application.getCreatedBy() == null || !application.getCreatedBy().getId().equals(chefDeProjet.getId())) {
            log.error("❌ Chef {} is not the creator of application {}", chefDeProjet.getId(), application.getId());
            throw new RuntimeException("You are not the creator of this application");
        }

        // Get admin
        User admin = getAdmin();
        if (admin == null) {
            log.error("❌ No admin found");
            throw new RuntimeException("No admin found");
        }
        log.info("Admin (target): {} (ID: {})", admin.getUsername(), admin.getId());

        // Check if there's already a pending request for this application
        List<Request> existingRequests = requestRepository.findByApplicationAndStatus(application, "PENDING");
        if (!existingRequests.isEmpty()) {
            log.warn("Found {} pending requests for this application", existingRequests.size());
            // You might want to prevent creating another one, or allow it
        }

        Request request = new Request();
        request.setRequestType("REASSIGNMENT_REQUEST_FROM_CHEF"); // New type
        request.setStatus("PENDING");
        request.setRequester(chefDeProjet);
        request.setTargetUser(admin);
        request.setApplication(application);
        request.setConvention(convention);
        request.setOldConvention(convention);
        request.setRecommendedChef(recommendedChef);
        request.setReason(reason);
        request.setRecommendations(recommendations);

        Request savedRequest = requestRepository.save(request);
        log.info("✅ Request saved with ID: {}", savedRequest.getId());

        // Send email notification to admin
        sendReassignmentRequestFromChefToAdminEmail(chefDeProjet, recommendedChef, application, savedRequest, admin);

        log.info("========== END CREATING REASSIGNMENT REQUEST FROM CHEF ==========");
        return savedRequest;
    }

    /**
     * Send email to admin about reassignment request from chef
     */
    private void sendReassignmentRequestFromChefToAdminEmail(User chef, User recommendedChef,
                                                             Application application,
                                                             Request request, User admin) {
        String subject = "📋 Demande de réassignation - " + application.getCode();

        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                "<div style='background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                "<h2 style='margin: 0;'>📋 Demande de réassignation</h2>" +
                "</div>" +
                "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                "<p>Bonjour Administrateur,</p>" +
                "<p>Le chef de projet <strong>" + chef.getFirstName() + " " + chef.getLastName() + "</strong> a demandé à être réassigné de l'application <strong>" +
                application.getName() + "</strong> en raison de sa charge de travail.</p>";

        if (recommendedChef != null) {
            content += "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                    "<h3 style='margin-top: 0; color: #333;'>Chef recommandé :</h3>" +
                    "<p><strong>" + recommendedChef.getFirstName() + " " + recommendedChef.getLastName() + "</strong></p>" +
                    "</div>";
        }

        content += "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                "<h3 style='margin-top: 0; color: #333;'>Raison :</h3>" +
                "<p>" + request.getReason() + "</p>" +
                "</div>";

        if (request.getRecommendations() != null && !request.getRecommendations().isEmpty()) {
            content += "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                    "<h3 style='margin-top: 0; color: #333;'>Recommandations :</h3>" +
                    "<p>" + request.getRecommendations() + "</p>" +
                    "</div>";
        }

        content += "<p>Veuillez examiner cette demande dans l'onglet 'Demandes'.</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='http://localhost:4200/requests' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir les demandes</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";

        sendEmail(admin, subject, content, admin);
        log.info("✅ Reassignment request email sent to admin");
    }
}
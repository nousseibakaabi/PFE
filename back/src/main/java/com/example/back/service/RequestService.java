// RequestService.java
package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.RequestActionDTO;
import com.example.back.payload.response.MailResponse;
import com.example.back.payload.response.RequestResponse;
import com.example.back.repository.*;
import com.example.back.service.mapper.RequestMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ConventionRepository conventionRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private RequestMapper requestMapper;

    @Autowired
    private HistoryService historyService;

    /**
     * Create a renewal acceptance request for chef de projet (when app created by admin)
     */
    @Transactional
    public Request createRenewalAcceptanceRequest(Convention oldConvention, Convention newConvention, User chefDeProjet, User admin){
        log.info("Creating renewal acceptance request for chef {} for convention {}", chefDeProjet.getUsername(), oldConvention.getReferenceConvention());

        Request request = new Request();
        request.setRequestType("RENEWAL_ACCEPTANCE");
        request.setStatus("PENDING");
        request.setRequester(admin);
        request.setTargetUser(chefDeProjet);
        request.setApplication(oldConvention.getApplication());
        request.setConvention(newConvention);
        request.setOldConvention(oldConvention);
        request.setReason("La convention a été renouvelée. Voulez-vous continuer à travailler sur cette application ?");

        Request savedRequest = requestRepository.save(request);

        // Send email notification to chef de projet
        sendRenewalAcceptanceRequestEmail(chefDeProjet, oldConvention, newConvention, savedRequest);

        return savedRequest;
    }

    /**
     * Create a reassignment suggestion from chef to admin
     */
    @Transactional
    public Request createReassignmentSuggestion(Convention oldConvention, Convention newConvention,
                                                User chefDeProjet, User recommendedChef,
                                                String reason, String recommendations) {
        log.info("Creating reassignment suggestion from {} for chef {}", chefDeProjet.getUsername(), recommendedChef.getUsername());

        Request request = new Request();
        request.setRequestType("REASSIGNMENT_SUGGESTION");
        request.setStatus("PENDING");
        request.setRequester(chefDeProjet);
        request.setTargetUser(null); // Target is admin
        request.setApplication(oldConvention.getApplication());
        request.setConvention(newConvention);
        request.setOldConvention(oldConvention);
        request.setRecommendedChef(recommendedChef);
        request.setReason(reason);
        request.setRecommendations(recommendations);

        Request savedRequest = requestRepository.save(request);

        // Send email to admin
        sendSuggestionToAdminEmail(chefDeProjet, recommendedChef, oldConvention, savedRequest);

        return savedRequest;
    }

    /**
     * Process request action (approve/deny)
     */
    @Transactional
    public RequestResponse processRequest(RequestActionDTO actionDTO, User processor) {
        Request request = requestRepository.findById(actionDTO.getRequestId())
                .orElseThrow(() -> new RuntimeException("Request not found"));

        boolean isAdmin = processor.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));

        if ("RENEWAL_ACCEPTANCE".equals(request.getRequestType())) {
            // Target user (chef de projet) processing their own request
            if (!request.getTargetUser().getId().equals(processor.getId())) {
                throw new RuntimeException("You can only process your own requests");
            }

            if ("APPROVE".equals(actionDTO.getAction())) {
                request.setStatus("APPROVED");
                request.setProcessedAt(LocalDateTime.now());

                // Send confirmation email
                sendRenewalApprovedEmail(processor, request);

            } else if ("DENY".equals(actionDTO.getAction())) {
                request.setStatus("DENIED");
                request.setDenialReason(actionDTO.getReason());
                request.setProcessedAt(LocalDateTime.now());

                // Send denial email to admin with reason
                sendRenewalDeniedEmail(processor, request, actionDTO.getReason());
            }

        } else if ("REASSIGNMENT_SUGGESTION".equals(request.getRequestType())) {
            // Admin processing chef's suggestion
            if (!isAdmin) {
                throw new RuntimeException("Only admin can process reassignment suggestions");
            }

            if ("APPROVE".equals(actionDTO.getAction())) {
                // Check if recommended chef can accept
                if (actionDTO.getRecommendedChefId() != null) {
                    User recommendedChef = userRepository.findById(actionDTO.getRecommendedChefId())
                            .orElseThrow(() -> new RuntimeException("Recommended chef not found"));

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

                    request.setStatus("APPROVED");
                    request.setRecommendedChef(recommendedChef);

                    // Send emails to both chefs
                    sendReassignmentApprovedEmails(request, recommendedChef, oldChef);
                }

            } else if ("DENY".equals(actionDTO.getAction())) {
                request.setStatus("DENIED");
                request.setDenialReason(actionDTO.getReason());
                request.setProcessedAt(LocalDateTime.now());

                // Send denial email to chef
                sendReassignmentDeniedEmail(request, actionDTO.getReason());
            }
        }

        request.setProcessedAt(LocalDateTime.now());
        Request updatedRequest = requestRepository.save(request);

        return requestMapper.toResponse(updatedRequest);
    }

    /**
     * Get all requests for current user
     */
    public List<RequestResponse> getUserRequests(User user) {
        List<Request> requests = requestRepository.findUserRequests(user);

        return requests.stream()
                .map(requestMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get requests by status
     */
    public List<RequestResponse> getRequestsByStatus(String status) {
        return requestRepository.findByStatus(status).stream()
                .map(requestMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if chef can accept the application based on workload
     */
    private boolean checkChefWorkload(User chef, Application application) {
        // Implement workload check logic
        // This should check if chef has capacity for this application
        return true; // Placeholder
    }

    // ============= EMAIL METHODS =============

    private void sendRenewalAcceptanceRequestEmail(User chef, Convention oldConv, Convention newConv, Request request) {
        String subject = "🔄 Renouvellement de convention - " + oldConv.getReferenceConvention();

        String content = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                        "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                        "<h2 style='margin: 0;'>🔄 Demande de renouvellement</h2>" +
                        "</div>" +
                        "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                        "<p>Bonjour <strong>%s %s</strong>,</p>" +
                        "<p>La convention <strong>%s</strong> a été renouvelée. Souhaitez-vous continuer à travailler sur cette application ?</p>" +
                        "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                        "<h3 style='margin-top: 0; color: #333;'>Détails :</h3>" +
                        "<p><strong>Application :</strong> %s - %s</p>" +
                        "<p><strong>Ancienne convention :</strong> %s</p>" +
                        "<p><strong>Nouvelle convention :</strong> %s</p>" +
                        "</div>" +
                        "<p>Veuillez vous connecter pour répondre à cette demande.</p>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                        "<a href='http://localhost:4200/requests' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir la demande</a>" +
                        "</div>" +
                        "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                chef.getFirstName(), chef.getLastName(),
                oldConv.getReferenceConvention(),
                oldConv.getApplication().getCode(), oldConv.getApplication().getName(),
                oldConv.getReferenceConvention(),
                newConv.getReferenceConvention()
        );

        sendEmail(chef, subject, content);
    }

    private void sendRenewalApprovedEmail(User chef, Request request) {
        String subject = "✅ Demande approuvée - Renouvellement de convention";

        String content = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                        "<div style='background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                        "<h2 style='margin: 0;'>✅ Demande approuvée</h2>" +
                        "</div>" +
                        "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                        "<p>Bonjour <strong>%s %s</strong>,</p>" +
                        "<p>Votre demande pour la convention <strong>%s</strong> a été approuvée.</p>" +
                        "<p>Vous êtes réassigné à cette application suite au renouvellement.</p>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                        "<a href='http://localhost:4200/applications/%d' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir l'application</a>" +
                        "</div>" +
                        "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                chef.getFirstName(), chef.getLastName(),
                request.getConvention().getReferenceConvention(),
                request.getApplication().getId()
        );

        sendEmail(chef, subject, content);
    }

    private void sendRenewalDeniedEmail(User chef, Request request, String reason) {
        // Find admin
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")))
                .findFirst()
                .orElse(null);

        if (admin != null) {
            String subject = "❌ Demande refusée - " + request.getConvention().getReferenceConvention();

            String content = String.format(
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head><meta charset='UTF-8'></head>" +
                            "<body style='font-family: Arial, sans-serif;'>" +
                            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                            "<div style='background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                            "<h2 style='margin: 0;'>❌ Demande refusée</h2>" +
                            "</div>" +
                            "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                            "<p>Bonjour Administrateur,</p>" +
                            "<p>Le chef de projet <strong>%s %s</strong> a refusé de continuer à travailler sur l'application <strong>%s</strong>.</p>" +
                            "<div style='background: #fee; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                            "<h3 style='margin-top: 0; color: #dc2626;'>Raison du refus :</h3>" +
                            "<p>%s</p>" +
                            "</div>" +
                            "<p>Veuillez assigner un autre chef de projet.</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='http://localhost:4200/applications/%d' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Assigner un chef</a>" +
                            "</div>" +
                            "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>",
                    chef.getFirstName(), chef.getLastName(),
                    request.getApplication().getName(),
                    reason,
                    request.getApplication().getId()
            );

            sendEmail(admin, subject, content);
        }
    }

    private void sendSuggestionToAdminEmail(User chef, User recommendedChef, Convention oldConv, Request request) {
        User admin = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")))
                .findFirst()
                .orElse(null);

        if (admin != null) {
            String subject = "💡 Suggestion de réassignation - " + oldConv.getReferenceConvention();

            String content = String.format(
                    "<!DOCTYPE html>" +
                            "<html>" +
                            "<head><meta charset='UTF-8'></head>" +
                            "<body style='font-family: Arial, sans-serif;'>" +
                            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                            "<div style='background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                            "<h2 style='margin: 0;'>💡 Suggestion de réassignation</h2>" +
                            "</div>" +
                            "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                            "<p>Bonjour Administrateur,</p>" +
                            "<p>Le chef de projet <strong>%s %s</strong> ne peut pas continuer à travailler sur l'application <strong>%s</strong> et recommande :</p>" +
                            "<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                            "<h3 style='margin-top: 0; color: #333;'>Chef recommandé :</h3>" +
                            "<p><strong>%s %s</strong></p>" +
                            "<p><strong>Raison :</strong> %s</p>" +
                            "<p><strong>Recommandations :</strong> %s</p>" +
                            "</div>" +
                            "<p>Veuillez examiner cette suggestion.</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='http://localhost:4200/requests' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir la demande</a>" +
                            "</div>" +
                            "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>",
                    chef.getFirstName(), chef.getLastName(),
                    oldConv.getApplication().getName(),
                    recommendedChef.getFirstName(), recommendedChef.getLastName(),
                    request.getReason(),
                    request.getRecommendations()
            );

            sendEmail(admin, subject, content);
        }
    }

    private void sendReassignmentApprovedEmails(Request request, User newChef, User oldChef) {
        // Email to old chef
        String subjectToOld = "✅ Suggestion approuvée - Nouveau chef assigné";

        String contentToOld = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                        "<div style='background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                        "<h2 style='margin: 0;'>✅ Suggestion approuvée</h2>" +
                        "</div>" +
                        "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                        "<p>Bonjour <strong>%s %s</strong>,</p>" +
                        "<p>Votre suggestion de réassignation pour l'application <strong>%s</strong> a été approuvée.</p>" +
                        "<p><strong>%s %s</strong> a été assigné comme nouveau chef de projet.</p>" +
                        "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                oldChef.getFirstName(), oldChef.getLastName(),
                request.getApplication().getName(),
                newChef.getFirstName(), newChef.getLastName()
        );

        sendEmail(oldChef, subjectToOld, contentToOld);

        // Email to new chef
        String subjectToNew = "📋 Nouvelle assignation suite à renouvellement";

        String contentToNew = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                        "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                        "<h2 style='margin: 0;'>📋 Nouvelle assignation</h2>" +
                        "</div>" +
                        "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                        "<p>Bonjour <strong>%s %s</strong>,</p>" +
                        "<p>Vous avez été assigné comme Chef de Projet pour l'application <strong>%s</strong> suite au renouvellement de sa convention.</p>" +
                        "<p><strong>Recommandation :</strong> %s</p>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                        "<a href='http://localhost:4200/applications/%d' style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Voir l'application</a>" +
                        "</div>" +
                        "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                newChef.getFirstName(), newChef.getLastName(),
                request.getApplication().getName(),
                request.getRecommendations() != null ? request.getRecommendations() : "Aucune",
                request.getApplication().getId()
        );

        sendEmail(newChef, subjectToNew, contentToNew);
    }

    private void sendReassignmentDeniedEmail(Request request, String reason) {
        User chef = request.getRequester();

        String subject = "❌ Suggestion refusée";

        String content = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><meta charset='UTF-8'></head>" +
                        "<body style='font-family: Arial, sans-serif;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; border-radius: 10px;'>" +
                        "<div style='background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); color: white; padding: 20px; border-radius: 10px 10px 0 0;'>" +
                        "<h2 style='margin: 0;'>❌ Suggestion refusée</h2>" +
                        "</div>" +
                        "<div style='background: white; padding: 20px; border-radius: 0 0 10px 10px;'>" +
                        "<p>Bonjour <strong>%s %s</strong>,</p>" +
                        "<p>Votre suggestion de réassignation pour l'application <strong>%s</strong> a été refusée.</p>" +
                        "<div style='background: #fee; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                        "<h3 style='margin-top: 0; color: #dc2626;'>Raison du refus :</h3>" +
                        "<p>%s</p>" +
                        "</div>" +
                        "<p style='color: #666; font-size: 12px; margin-top: 20px;'>Cet email a été envoyé automatiquement.</p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>",
                chef.getFirstName(), chef.getLastName(),
                request.getApplication().getName(),
                reason
        );

        sendEmail(chef, subject, content);
    }

    private void sendEmail(User recipient, String subject, String content) {
        try {
            User systemSender = userRepository.findByUsername("system")
                    .orElseGet(() -> {
                        // Fallback to first admin
                        return userRepository.findAll().stream()
                                .filter(u -> u.getRoles().stream()
                                        .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")))
                                .findFirst()
                                .orElse(null);
                    });

            if (systemSender != null) {
                com.example.back.payload.request.MailRequest request = new com.example.back.payload.request.MailRequest();
                request.setSubject(subject);
                request.setContent(content);
                request.setTo(List.of(recipient.getEmail()));
                request.setImportance("NORMAL");

                mailService.sendMail(request, systemSender, null);
                log.info("Email sent to {}: {}", recipient.getEmail(), subject);
            }
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
    }
}
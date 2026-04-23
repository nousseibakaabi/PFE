package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.MailActionRequest;
import com.example.back.payload.request.MailDraftRequest;
import com.example.back.payload.request.MailRequest;
import com.example.back.payload.response.MailDraftResponse;
import com.example.back.payload.response.MailResponse;
import com.example.back.payload.response.MailStatsResponse;
import com.example.back.repository.*;
import com.example.back.service.mapper.MailMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class MailService {

    private final MailRepository mailRepository;

    private final MailRecipientRepository recipientRepository;

    private final MailAttachmentRepository attachmentRepository;

    private final MailDraftRepository draftRepository;

    private final UserRepository userRepository;

    private final MailDraftAttachmentRepository draftAttachmentRepository;

    private final ObjectMapper objectMapper;

    private final MailMapper mailMapper;

    private final MailGroupService groupService;

    private final MailFolderService folderService;

    private final MailGroupRepository groupRepository;

    @Value("${app.upload.dir:${user.home}/uploads/mails}")
    private String uploadDir;

    public MailService(MailAttachmentRepository attachmentRepository, MailRepository mailRepository, MailRecipientRepository recipientRepository, MailDraftRepository draftRepository, MailGroupRepository groupRepository, UserRepository userRepository, MailDraftAttachmentRepository draftAttachmentRepository, ObjectMapper objectMapper, MailFolderService folderService, MailMapper mailMapper, MailGroupService groupService) {
        this.attachmentRepository = attachmentRepository;
        this.mailRepository = mailRepository;
        this.recipientRepository = recipientRepository;
        this.draftRepository = draftRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.draftAttachmentRepository = draftAttachmentRepository;
        this.objectMapper = objectMapper;
        this.folderService = folderService;
        this.mailMapper = mailMapper;
        this.groupService = groupService;
    }

    // ============= SEND EMAIL =============

    @Transactional
    public MailResponse sendMail(MailRequest request, User sender, List<MultipartFile> files) throws IOException {
        log.info("========== MAIL SENDING DEBUG ==========");
        log.info("Sending mail from: {} with subject: {}", sender.getEmail(), request.getSubject());
        log.info("TO recipients: {}", request.getTo());
        log.info("CC recipients: {}", request.getCc());
        log.info("BCC recipients: {}", request.getBcc());

        // Create mail entity
        Mail mail = new Mail();
        mail.setSubject(request.getSubject());
        mail.setContent(request.getContent());
        mail.setSender(sender);
        mail.setSenderEmail(sender.getEmail());
        mail.setSenderName(sender.getFirstName() + " " + sender.getLastName());
        mail.setImportance(request.getImportance() != null ? request.getImportance() : "NORMAL");
        mail.setHasAttachments(files != null && !files.isEmpty());

        // Set parent mail if it's a reply
        if (request.getParentMailId() != null) {
            Mail parentMail = mailRepository.findById(request.getParentMailId())
                    .orElseThrow(() -> new RuntimeException("Parent mail not found"));
            mail.setParentMail(parentMail);
        }

        // Save mail first to get ID
        Mail savedMail = mailRepository.save(mail);
        log.info("Mail saved with ID: {}", savedMail.getId());

        // Add recipients - each with their own status
        List<MailRecipient> allRecipients = new ArrayList<>();

        // Add sender as a recipient too (for Sent folder)
        MailRecipient senderRecipient = new MailRecipient();
        senderRecipient.setMail(savedMail);
        senderRecipient.setEmail(sender.getEmail());
        senderRecipient.setName(sender.getFirstName() + " " + sender.getLastName());
        senderRecipient.setUser(sender);
        senderRecipient.setType("FROM");
        senderRecipient.setIsRead(true);
        senderRecipient.setIsStarred(false);
        senderRecipient.setIsArchived(false);
        senderRecipient.setIsDeleted(false);
        allRecipients.add(senderRecipient);
        log.info("Added sender recipient: {}", sender.getEmail());

        // TO recipients
        if (request.getTo() != null && !request.getTo().isEmpty()) {
            log.info("Processing TO recipients: {}", request.getTo());
            List<MailRecipient> toRecipients = createRecipients(savedMail, request.getTo(), "TO", sender);
            log.info("Created {} TO recipients", toRecipients.size());
            allRecipients.addAll(toRecipients);
        }

        // CC recipients
        if (request.getCc() != null && !request.getCc().isEmpty()) {
            log.info("Processing CC recipients: {}", request.getCc());
            List<MailRecipient> ccRecipients = createRecipients(savedMail, request.getCc(), "CC", sender);
            log.info("Created {} CC recipients", ccRecipients.size());
            allRecipients.addAll(ccRecipients);
        }

        // BCC recipients
        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            log.info("Processing BCC recipients: {}", request.getBcc());
            List<MailRecipient> bccRecipients = createRecipients(savedMail, request.getBcc(), "BCC", sender);
            log.info("Created {} BCC recipients", bccRecipients.size());
            allRecipients.addAll(bccRecipients);
        }

        log.info("Total recipients to save: {}", allRecipients.size());
        recipientRepository.saveAll(allRecipients);
        log.info("Recipients saved successfully");

        savedMail.setRecipients(allRecipients);

        // Handle attachments
        if (files != null && !files.isEmpty()) {
            List<MailAttachment> attachments = saveAttachments(savedMail, files);
            attachmentRepository.saveAll(attachments);
            savedMail.setAttachments(attachments);
            log.info("Saved {} attachments", attachments.size());
        }

        log.info("Mail sent successfully with ID: {}", savedMail.getId());
        log.info("========== END MAIL DEBUG ==========");

        return mailMapper.toResponse(savedMail, sender.getEmail());
    }

    // ============= SYSTEM NOTIFICATION EMAILS =============

    /**
     * Send facture due notification through the integrated mail system
     */
    @Transactional
    public MailResponse sendFactureDueNotification(User recipient, Facture facture, int daysUntilDue) {
        log.info("Sending facture due notification to: {} for facture: {}", recipient.getEmail(), facture.getNumeroFacture());

        // Create the mail subject based on days until due
        String subject;
        if (daysUntilDue > 0) {
            subject = daysUntilDue == 1 ?
                    "🔔 Rappel: Facture due demain - " + facture.getNumeroFacture() :
                    "🔔 Rappel: Facture due dans " + daysUntilDue + " jours - " + facture.getNumeroFacture();
        } else if (daysUntilDue == 0) {
            subject = "⚠️ URGENT: Facture due aujourd'hui - " + facture.getNumeroFacture();
        } else {
            subject = "🚨 ALERTE: Facture en retard de " + Math.abs(daysUntilDue) + " jours - " + facture.getNumeroFacture();
        }

        // Build the HTML content
        String content = buildFactureDueEmailContent(recipient, facture, daysUntilDue);

        User systemSender = getSystemSender();

        // Create mail request
        MailRequest request = new MailRequest();
        request.setSubject(subject);
        request.setContent(content);
        request.setImportance(getImportanceLevel(daysUntilDue));
        request.setTo(Arrays.asList(recipient.getEmail()));

        try {
            // Send through the integrated mail system
            return sendMail(request, systemSender, null);
        } catch (IOException e) {
            log.error("Failed to send facture due notification: {}", e.getMessage());
            throw new RuntimeException("Failed to send notification email", e);
        }
    }

    /**
     * Send facture due notification to multiple recipients
     */
    @Transactional
    public List<MailResponse> sendFactureDueNotificationToMultiple(List<User> recipients, Facture facture, int daysUntilDue) {
        List<MailResponse> responses = new ArrayList<>();
        for (User recipient : recipients) {
            try {
                MailResponse response = sendFactureDueNotification(recipient, facture, daysUntilDue);
                responses.add(response);
            } catch (Exception e) {
                log.error("Failed to send notification to {}: {}", recipient.getEmail(), e.getMessage());
            }
        }
        return responses;
    }

    /**
     * Build HTML content for facture due email
     */
    private String buildFactureDueEmailContent(User recipient, Facture facture, int daysUntilDue) {
        Convention convention = facture.getConvention();
        Application application = convention != null ? convention.getApplication() : null;

        // Get status color and icon
        String statusColor;
        String statusIcon;
        String statusText;

        if (daysUntilDue > 0) {
            statusColor = daysUntilDue <= 2 ? "#f59e0b" : "#3b82f6"; // Orange for 1-2 days, Blue for 3-5 days
            statusIcon = "⏰";
            statusText = daysUntilDue == 1 ? "Due demain" : "Due dans " + daysUntilDue + " jours";
        } else if (daysUntilDue == 0) {
            statusColor = "#ef4444"; // Red for today
            statusIcon = "⚠️";
            statusText = "Due aujourd'hui";
        } else {
            statusColor = "#dc2626"; // Dark red for overdue
            statusIcon = "🚨";
            statusText = "En retard de " + Math.abs(daysUntilDue) + " jours";
        }

        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html>");
        content.append("<head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        content.append("<style>");
        content.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #1f2937; margin: 0; padding: 0; }");
        content.append(".email-wrapper { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); }");
        content.append(".header { padding: 24px 32px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }");
        content.append(".header h1 { margin: 0; font-size: 24px; font-weight: 600; }");
        content.append(".header p { margin: 8px 0 0; opacity: 0.9; font-size: 14px; }");
        content.append(".content { padding: 32px; }");
        content.append(".greeting { font-size: 16px; margin-bottom: 24px; }");
        content.append(".status-badge { display: inline-block; padding: 8px 16px; border-radius: 9999px; font-weight: 600; font-size: 14px; margin-bottom: 24px; background-color: ").append(statusColor).append("; color: white; }");
        content.append(".facture-card { background-color: #f9fafb; border-radius: 8px; padding: 24px; margin: 24px 0; border-left: 4px solid ").append(statusColor).append("; }");
        content.append(".facture-card h2 { margin: 0 0 16px; font-size: 18px; color: #374151; }");
        content.append(".detail-row { display: flex; margin-bottom: 12px; }");
        content.append(".detail-label { width: 140px; font-weight: 500; color: #6b7280; }");
        content.append(".detail-value { flex: 1; color: #1f2937; font-weight: 500; }");
        content.append(".highlight { color: ").append(statusColor).append("; font-weight: 700; }");
        content.append(".amount { font-size: 24px; font-weight: 700; color: #059669; }");
        content.append(".actions { margin-top: 32px; text-align: center; }");
        content.append(".button { display: inline-block; padding: 12px 24px; background-color: #3b82f6; color: white; text-decoration: none; border-radius: 6px; font-weight: 500; margin: 0 8px; }");
        content.append(".button:hover { background-color: #2563eb; }");
        content.append(".footer { padding: 24px 32px; border-top: 1px solid #e5e7eb; font-size: 12px; color: #9ca3af; text-align: center; background-color: #f9fafb; }");
        content.append("</style>");
        content.append("</head>");
        content.append("<body>");
        content.append("<div class='email-wrapper'>");

        // Header
        content.append("<div class='header'>");
        content.append("<h1>").append(statusIcon).append(" Rappel de Facture</h1>");
        content.append("<p>Système de gestion des factures</p>");
        content.append("</div>");

        // Content
        content.append("<div class='content'>");
        content.append("<div class='greeting'>");
        content.append("<p>Bonjour <strong>").append(recipient.getFirstName()).append(" ").append(recipient.getLastName()).append("</strong>,</p>");
        content.append("</div>");

        // Status badge
        content.append("<div class='status-badge'>").append(statusIcon).append(" ").append(statusText).append("</div>");

        // Message based on status
        if (daysUntilDue > 0) {
            if (daysUntilDue == 1) {
                content.append("<p>Une facture sera due <span class='highlight'>demain</span>. Veuillez prendre les mesures nécessaires.</p>");
            } else {
                content.append("<p>Une facture sera due dans <span class='highlight'>").append(daysUntilDue).append(" jours</span>. Veuillez planifier le paiement.</p>");
            }
        } else if (daysUntilDue == 0) {
            content.append("<p><strong>⚠️ Attention:</strong> Une facture est due <span class='highlight'>aujourd'hui</span>. Un paiement immédiat est requis.</p>");
        } else {
            content.append("<p><strong>🚨 Urgent:</strong> Une facture est en retard de <span class='highlight'>").append(Math.abs(daysUntilDue)).append(" jours</span>. Des pénalités peuvent s'appliquer.</p>");
        }

        // Facture details card
        content.append("<div class='facture-card'>");
        content.append("<h2>Détails de la facture</h2>");

        content.append("<div class='detail-row'>");
        content.append("<span class='detail-label'>Numéro:</span>");
        content.append("<span class='detail-value'>").append(facture.getNumeroFacture()).append("</span>");
        content.append("</div>");

        if (convention != null) {
            content.append("<div class='detail-row'>");
            content.append("<span class='detail-label'>Convention:</span>");
            content.append("<span class='detail-value'>").append(convention.getReferenceConvention()).append("</span>");
            content.append("</div>");

            content.append("<div class='detail-row'>");
            content.append("<span class='detail-label'>Libellé:</span>");
            content.append("<span class='detail-value'>").append(convention.getLibelle()).append("</span>");
            content.append("</div>");
        }

        if (application != null) {
            content.append("<div class='detail-row'>");
            content.append("<span class='detail-label'>Application:</span>");
            content.append("<span class='detail-value'>").append(application.getCode()).append(" - ").append(application.getName()).append("</span>");
            content.append("</div>");

            content.append("<div class='detail-row'>");
            content.append("<span class='detail-label'>Client:</span>");
            content.append("<span class='detail-value'>").append(application.getClientName()).append("</span>");
            content.append("</div>");
        }

        content.append("<div class='detail-row'>");
        content.append("<span class='detail-label'>Date d'échéance:</span>");
        content.append("<span class='detail-value'>").append(
                facture.getDateEcheance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        ).append("</span>");
        content.append("</div>");

        content.append("<div class='detail-row'>");
        content.append("<span class='detail-label'>Montant TTC:</span>");
        content.append("<span class='detail-value amount'>").append(facture.getMontantTTC()).append(" TND</span>");
        content.append("</div>");

        content.append("</div>");

        // Actions
        content.append("<div class='actions'>");
        content.append("<a href='#' class='button' style='background-color: #10b981;'>✅ Marquer comme payée</a>");
        content.append("<a href='#' class='button' style='background-color: #3b82f6;'>👁️ Voir la facture</a>");
        content.append("</div>");

        content.append("</div>");

        // Footer
        content.append("<div class='footer'>");
        content.append("<p>Cet email a été envoyé automatiquement par le système de gestion.</p>");
        content.append("<p>© 2024 - Tous droits réservés</p>");
        content.append("</div>");

        content.append("</div>");
        content.append("</body>");
        content.append("</html>");

        return content.toString();
    }

    /**
     * Get the importance level based on days until due
     */
    private String getImportanceLevel(int daysUntilDue) {
        if (daysUntilDue <= 0) {
            return "HIGH";
        } else if (daysUntilDue <= 2) {
            return "HIGH";
        } else {
            return "NORMAL";
        }
    }

    /**
     * Get system sender user
     */
    private User getSystemSender() {

        return userRepository.findByUsername("system")
                .orElseGet(() -> {
                    throw new RuntimeException("No system user found for sending notifications");
                });
    }

    // ============= EXISTING METHODS =============

    private List<MailRecipient> createRecipients(Mail mail, List<String> emails, String type, User currentUser) {
        log.info("createRecipients called with type: {}, emails: {}", type, emails);

        List<String> expandedEmails = expandGroups(emails, currentUser);
        log.info("Expanded emails: {}", expandedEmails);

        List<MailRecipient> recipients = new ArrayList<>();

        // First, add the group identifiers themselves
        for (String email : emails) {
            if (email != null && email.startsWith("GROUP:")) {
                MailRecipient groupRecipient = new MailRecipient();
                groupRecipient.setMail(mail);
                groupRecipient.setEmail(email.trim());
                groupRecipient.setType(type);
                groupRecipient.setIsRead(false);
                groupRecipient.setIsStarred(false);
                groupRecipient.setIsArchived(false);
                groupRecipient.setIsDeleted(false);
                recipients.add(groupRecipient);
                log.info("Added group recipient: {}", email);
            }
        }

        // Then add all expanded individual emails
        for (String email : expandedEmails) {
            // Skip if this email is already added as a group recipient
            if (recipients.stream().anyMatch(r -> r.getEmail().equals(email))) {
                log.info("Skipping duplicate email: {}", email);
                continue;
            }

            MailRecipient recipient = new MailRecipient();
            recipient.setMail(mail);
            recipient.setEmail(email.trim());
            recipient.setType(type);
            recipient.setIsRead(false);
            recipient.setIsStarred(false);
            recipient.setIsArchived(false);
            recipient.setIsDeleted(false);

            log.info("Looking up user by email: {}", email.trim());
            Optional<User> userOpt = userRepository.findByEmail(email.trim());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                recipient.setName(user.getFirstName() + " " + user.getLastName());
                recipient.setUser(user);
                log.info("Found user: {} with ID: {}", user.getUsername(), user.getId());
            } else {
                log.warn("No user found with email: {}", email);
                // Still add the recipient but without user reference
                recipient.setName(email.trim());
            }

            recipients.add(recipient);
            log.info("Added recipient: {} with type: {}", email, type);
        }

        log.info("createRecipients returning {} recipients", recipients.size());
        return recipients;
    }


    private List<MailAttachment> saveAttachments(Mail mail, List<MultipartFile> files) throws IOException {
        List<MailAttachment> attachments = new ArrayList<>();

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // Create attachment entity
            MailAttachment attachment = new MailAttachment();
            attachment.setMail(mail);
            attachment.setFileName(originalFilename);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setFilePath(filePath.toString());

            attachments.add(attachment);
        }

        return attachments;
    }

    // ============= SAVE DRAFT =============

    @Transactional
    public MailDraft saveDraft(MailDraft draft, User user) {
        draft.setUser(user);
        draft.setLastSavedAt(LocalDateTime.now());
        return draftRepository.save(draft);
    }

    // ============= GET INBOX =============

    public Page<MailResponse> getInbox(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Mail> mails = mailRepository.findInboxByRecipientEmail(userEmail, pageable);
        return mails.map(mail -> mailMapper.toResponse(mail, userEmail));
    }

    // ============= GET SENT =============

    public Page<MailResponse> getSent(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Mail> mails = mailRepository.findBySenderEmailAndIsDeletedFalseOrderBySentAtDesc(userEmail, pageable);
        return mails.map(mail -> mailMapper.toResponse(mail, userEmail));
    }

    // ============= GET STARRED =============

    public Page<MailResponse> getStarred(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Mail> mails = mailRepository.findStarredByUserEmail(userEmail, pageable);
        return mails.map(mail -> mailMapper.toResponse(mail, userEmail));
    }

    // ============= GET ARCHIVED =============

    public Page<MailResponse> getArchived(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Mail> mails = mailRepository.findArchivedByUserEmail(userEmail, pageable);
        return mails.map(mail -> mailMapper.toResponse(mail, userEmail));
    }

    // ============= GET MAIL BY ID =============
    public MailResponse getMailById(Long id, String userEmail) {
        Mail mail = mailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        // Get the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user has access - either as sender or recipient
        boolean isSender = mail.getSender() != null && mail.getSender().getEmail().equals(userEmail);
        boolean isRecipient = mail.getRecipients().stream()
                .anyMatch(r -> r.getEmail() != null && r.getEmail().equals(userEmail));

        // Check if user is part of any group that received this mail
        boolean isInGroup = false;
        for (MailRecipient recipient : mail.getRecipients()) {
            if (recipient.getEmail() != null && recipient.getEmail().startsWith("GROUP:")) {
                String groupName = recipient.getEmail().substring(6);
                Optional<MailGroup> group = groupRepository.findByNameAndIsSystemTrue(groupName);
                if (group.isPresent() && group.get().getMembers().contains(user)) {
                    isInGroup = true;
                    break;
                }
            }
        }

        if (!isSender && !isRecipient && !isInGroup) {
            throw new RuntimeException("Access denied");
        }

        // Mark as read if user is recipient
        mail.getRecipients().stream()
                .filter(r -> r.getEmail() != null && r.getEmail().equals(userEmail) && !r.getIsRead())
                .findFirst()
                .ifPresent(recipient -> {
                    recipient.setIsRead(true);
                    recipient.setReadAt(LocalDateTime.now());
                    recipientRepository.save(recipient);
                });

        return mailMapper.toResponse(mail, userEmail);
    }

    // ============= SEARCH MAILS =============

    public Page<MailResponse> searchMails(String userEmail, String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Mail> mails = mailRepository.searchByUserEmail(userEmail, searchTerm, pageable);
        return mails.map(mail -> mailMapper.toResponse(mail, userEmail));
    }

    // ============= BATCH ACTIONS =============

    @Transactional
    public void performBatchAction(MailActionRequest request, String userEmail) {
        for (Long mailId : request.getMailIds()) {

            switch (request.getAction()) {
                case "READ":
                    markAsRead(mailId, userEmail);
                    break;
                case "UNREAD":
                    markAsUnread(mailId, userEmail);
                    break;
                case "STAR":
                    setStarred(mailId, userEmail, true);
                    break;
                case "UNSTAR":
                    setStarred(mailId, userEmail, false);
                    break;
                case "ARCHIVE":
                    setArchived(mailId, userEmail, true);
                    break;
                case "UNARCHIVE":
                    setArchived(mailId, userEmail, false);
                    break;
                case "DELETE":
                    moveToTrash(mailId, userEmail);
                    break;
                case "RESTORE":
                    restoreFromTrash(mailId, userEmail);
                    break;
                default:
                    log.warn("Unknown action: {}", request.getAction());
            }
        }
    }

    // ============= INDIVIDUAL ACTIONS =============

    @Transactional
    public void markAsRead(Long mailId, String userEmail) {
        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        mail.getRecipients().stream()
                .filter(r -> r.getEmail().equals(userEmail))
                .findFirst()
                .ifPresent(recipient -> {
                    recipient.setIsRead(true);
                    recipient.setReadAt(LocalDateTime.now());
                    recipientRepository.save(recipient);
                });
    }

    @Transactional
    public void markAsUnread(Long mailId, String userEmail) {
        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        mail.getRecipients().stream()
                .filter(r -> r.getEmail().equals(userEmail))
                .findFirst()
                .ifPresent(recipient -> {
                    recipient.setIsRead(false);
                    recipient.setReadAt(null);
                    recipientRepository.save(recipient);
                });
    }

    @Transactional
    public void setStarred(Long mailId, String userEmail, boolean starred) {
        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        mail.getRecipients().stream()
                .filter(r -> r.getEmail().equals(userEmail))
                .findFirst()
                .ifPresent(recipient -> {
                    recipient.setIsStarred(starred);
                    recipientRepository.save(recipient);
                });
    }

    @Transactional
    public void setArchived(Long mailId, String userEmail, boolean archived) {
        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        mail.getRecipients().stream()
                .filter(r -> r.getEmail().equals(userEmail))
                .findFirst()
                .ifPresent(recipient -> {
                    recipient.setIsArchived(archived);
                    recipientRepository.save(recipient);
                });
    }

    @Transactional
    public void moveToTrash(Long mailId, String userEmail) {
        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        mail.getRecipients().stream()
                .filter(r -> r.getEmail().equals(userEmail))
                .findFirst()
                .ifPresent(recipient -> {
                    recipient.setIsDeleted(true);
                    recipientRepository.save(recipient);
                });
    }

    @Transactional
    public void restoreFromTrash(Long mailId, String userEmail) {
        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        mail.getRecipients().stream()
                .filter(r -> r.getEmail().equals(userEmail))
                .findFirst()
                .ifPresent(recipient -> {
                    recipient.setIsDeleted(false);
                    recipientRepository.save(recipient);
                });
    }

    public Page<MailResponse> getTrash(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Mail> mails = mailRepository.findTrashByUserEmail(userEmail, pageable);
        return mails.map(mail -> mailMapper.toResponse(mail, userEmail));
    }

    @Transactional
    public void deletePermanently(Long mailId, String userEmail) {
        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        // Remove this recipient's association
        mail.getRecipients().removeIf(r -> r.getEmail().equals(userEmail));

        // If no recipients left, delete the mail entirely
        if (mail.getRecipients().isEmpty()) {
            // Delete attachments
            if (mail.getAttachments() != null) {
                for (MailAttachment attachment : mail.getAttachments()) {
                    try {
                        Files.deleteIfExists(Paths.get(attachment.getFilePath()));
                    } catch (IOException e) {
                        log.error("Failed to delete attachment file: {}", attachment.getFilePath(), e);
                    }
                }
            }
            mailRepository.delete(mail);
        } else {
            mailRepository.save(mail);
        }
    }

    @Transactional
    public MailDraft saveDraft(MailDraftRequest request, User user, List<MultipartFile> files) throws IOException {
        log.info("Saving draft for user: {}", user.getEmail());

        MailDraft draft = new MailDraft();
        draft.setUser(user);
        draft.setSubject(request.getSubject());
        draft.setContent(request.getContent());

        // Convert recipient lists to JSON strings
        if (request.getTo() != null && !request.getTo().isEmpty()) {
            draft.setToRecipients(objectMapper.writeValueAsString(request.getTo()));
        }

        if (request.getCc() != null && !request.getCc().isEmpty()) {
            draft.setCcRecipients(objectMapper.writeValueAsString(request.getCc()));
        }

        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            draft.setBccRecipients(objectMapper.writeValueAsString(request.getBcc()));
        }

        draft.setLastSavedAt(LocalDateTime.now());
        draft.setIsSending(false);

        // Save draft first
        MailDraft savedDraft = draftRepository.save(draft);

        // Handle attachments
        if (files != null && !files.isEmpty()) {
            List<MailDraftAttachment> attachments = saveDraftAttachments(savedDraft, files);
            draftAttachmentRepository.saveAll(attachments);
            savedDraft.setAttachments(attachments);
        }

        log.info("Draft saved successfully with ID: {}", savedDraft.getId());
        return savedDraft;
    }

    private List<MailDraftAttachment> saveDraftAttachments(MailDraft draft, List<MultipartFile> files) throws IOException {
        List<MailDraftAttachment> attachments = new ArrayList<>();

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir + "/drafts");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = "draft_" + UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath);

            // Create attachment entity
            MailDraftAttachment attachment = new MailDraftAttachment();
            attachment.setDraft(draft);
            attachment.setFileName(originalFilename);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setFilePath(filePath.toString());

            attachments.add(attachment);
        }

        return attachments;
    }

    // ============= GET DRAFT BY ID =============

    public MailDraft getDraftById(Long id, User user) {
        MailDraft draft = draftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Draft not found"));

        if (!draft.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return draft;
    }

    // ============= UPDATE DRAFT =============

    @Transactional
    public MailDraft updateDraft(Long id, MailDraftRequest request, User user, List<MultipartFile> files) throws IOException {
        MailDraft draft = getDraftById(id, user);

        draft.setSubject(request.getSubject());
        draft.setContent(request.getContent());

        // Update recipient JSON strings
        if (request.getTo() != null) {
            draft.setToRecipients(objectMapper.writeValueAsString(request.getTo()));
        }

        if (request.getCc() != null) {
            draft.setCcRecipients(objectMapper.writeValueAsString(request.getCc()));
        }

        if (request.getBcc() != null) {
            draft.setBccRecipients(objectMapper.writeValueAsString(request.getBcc()));
        }

        draft.setLastSavedAt(LocalDateTime.now());

        // Handle new attachments
        if (files != null && !files.isEmpty()) {
            List<MailDraftAttachment> newAttachments = saveDraftAttachments(draft, files);
            draftAttachmentRepository.saveAll(newAttachments);
            draft.getAttachments().addAll(newAttachments);
        }

        return draftRepository.save(draft);
    }

    // ============= DELETE DRAFT =============

    @Transactional
    public void deleteDraft(Long id, User user) {
        MailDraft draft = getDraftById(id, user);

        // Delete physical files
        if (draft.getAttachments() != null) {
            for (MailDraftAttachment attachment : draft.getAttachments()) {
                try {
                    Files.deleteIfExists(Paths.get(attachment.getFilePath()));
                } catch (IOException e) {
                    log.error("Failed to delete draft attachment: {}", attachment.getFilePath(), e);
                }
            }
        }

        draftRepository.delete(draft);
    }

    // ============= GET DRAFTS WITH RECIPIENTS PARSED =============

    public List<MailDraftResponse> getDraftsWithDetails(User user) {
        List<MailDraft> drafts = draftRepository.findByUserOrderByLastSavedAtDesc(user);
        List<MailDraftResponse> result = new ArrayList<>();

        for (MailDraft draft : drafts) {
            MailDraftResponse response = new MailDraftResponse();
            response.setId(draft.getId());
            response.setSubject(draft.getSubject());
            response.setContent(draft.getContent());
            response.setLastSavedAt(draft.getLastSavedAt());
            response.setIsSending(draft.getIsSending());

            // Parse recipients from JSON
            try {
                if (draft.getToRecipients() != null) {
                    response.setTo(objectMapper.readValue(draft.getToRecipients(), new TypeReference<List<String>>() {}));
                }
                if (draft.getCcRecipients() != null) {
                    response.setCc(objectMapper.readValue(draft.getCcRecipients(), new TypeReference<List<String>>() {}));
                }
                if (draft.getBccRecipients() != null) {
                    response.setBcc(objectMapper.readValue(draft.getBccRecipients(), new TypeReference<List<String>>() {}));
                }
            } catch (Exception e) {
                log.error("Error parsing draft recipients", e);
            }

            // Add attachments info
            List<Map<String, Object>> attachmentsInfo = new ArrayList<>();
            for (MailDraftAttachment attachment : draft.getAttachments()) {
                Map<String, Object> attInfo = new HashMap<>();
                attInfo.put("id", attachment.getId());
                attInfo.put("fileName", attachment.getFileName());
                attInfo.put("fileType", attachment.getFileType());
                attInfo.put("fileSize", attachment.getFileSize());
                attachmentsInfo.add(attInfo);
            }
            response.setAttachments(attachmentsInfo);

            result.add(response);
        }

        return result;
    }

    private List<String> expandGroups(List<String> recipients, User currentUser) {
        if (recipients == null) return new ArrayList<>();

        List<String> expanded = new ArrayList<>();
        for (String recipient : recipients) {
            if (recipient != null && recipient.startsWith("GROUP:")) {
                // Keep the group identifier for tracking
                expanded.add(recipient);

                // Also add individual members for actual delivery
                String groupName = recipient.substring(6);
                Optional<MailGroup> systemGroup = groupRepository.findByNameAndIsSystemTrue(groupName);
                if (systemGroup.isPresent()) {
                    systemGroup.get().getMembers().stream()
                            .map(User::getEmail)
                            .forEach(expanded::add);
                } else if (currentUser != null) {
                    Optional<MailGroup> customGroup = groupRepository.findByNameAndOwner(groupName, currentUser);
                    if (customGroup.isPresent()) {
                        customGroup.get().getMembers().stream()
                                .map(User::getEmail)
                                .forEach(expanded::add);
                    }
                }
            } else if (recipient != null) {
                expanded.add(recipient.trim());
            }
        }
        return expanded;
    }

    public Page<MailResponse> getGroupMails(Long groupId, String userEmail, int page, int size) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return folderService.getMailsForGroup(groupId, user, page, size);
    }

    public MailStatsResponse getStats(String userEmail) {
        MailStatsResponse stats = new MailStatsResponse();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Regular mail stats
        stats.setInboxCount(mailRepository.findInboxByRecipientEmail(userEmail, Pageable.unpaged()).getTotalElements());
        stats.setUnreadCount(mailRepository.countUnreadByRecipientEmail(userEmail));
        stats.setSentCount(mailRepository.findBySenderEmailAndIsDeletedFalseOrderBySentAtDesc(userEmail, Pageable.unpaged()).getTotalElements());
        stats.setStarredCount(mailRepository.findStarredByUserEmail(userEmail, Pageable.unpaged()).getTotalElements());
        stats.setArchivedCount(mailRepository.findArchivedByUserEmail(userEmail, Pageable.unpaged()).getTotalElements());
        stats.setTrashCount(mailRepository.findTrashByUserEmail(userEmail, Pageable.unpaged()).getTotalElements());

        stats.setDraftCount(draftRepository.countByUser(user));

        // Group statistics (counts of groups)
        long systemGroupsCount = groupRepository.countByIsSystemTrue();
        long customGroupsCount = groupRepository.countCustomGroupsForUser(user);
        stats.setSystemGroupsCount(systemGroupsCount);
        stats.setCustomGroupsCount(customGroupsCount);
        stats.setTotalGroupsCount(systemGroupsCount + customGroupsCount);

        // Group mail statistics
        long totalGroupMails = mailRepository.countAllGroupMailsForUser(user);
        long unreadGroupMails = mailRepository.countUnreadGroupMailsForUser(user);

        stats.setGroupMailsCount(totalGroupMails);
        stats.setUnreadGroupMailsCount(unreadGroupMails);

        // Detailed per-group statistics (optional)
        List<MailStatsResponse.GroupMailStats> groupStats = groupService.getGroupMailStats(user);
        stats.setGroupStats(groupStats);

        return stats;
    }
}
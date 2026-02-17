// MailService.java
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
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MailService {

    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private MailRecipientRepository recipientRepository;

    @Autowired
    private MailAttachmentRepository attachmentRepository;

    @Autowired
    private MailDraftRepository draftRepository;

    @Autowired
    private MailFolderRepository folderRepository;

    @Autowired
    private MailSignatureRepository signatureRepository;

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private MailDraftAttachmentRepository draftAttachmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MailMapper mailMapper;

    @Autowired
    private MailGroupService groupService;

    @Autowired
    private MailFolderService folderService;


    @Autowired
    private MailGroupRepository groupRepository;

    @Value("${app.upload.dir:${user.home}/uploads/mails}")
    private String uploadDir;

    // ============= SEND EMAIL =============


    @Transactional
    public MailResponse sendMail(MailRequest request, User sender, List<MultipartFile> files) throws IOException {
        log.info("Sending mail from: {} with subject: {}", sender.getEmail(), request.getSubject());

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

        // Add recipients - each with their own status
        List<MailRecipient> allRecipients = new ArrayList<>();

        // Add sender as a recipient too (for Sent folder)
        MailRecipient senderRecipient = new MailRecipient();
        senderRecipient.setMail(savedMail);
        senderRecipient.setEmail(sender.getEmail());
        senderRecipient.setName(sender.getFirstName() + " " + sender.getLastName());
        senderRecipient.setUser(sender);
        senderRecipient.setType("FROM"); // Special type for sender
        senderRecipient.setIsRead(true); // Sender always sees as read
        senderRecipient.setIsStarred(false);
        senderRecipient.setIsArchived(false);
        senderRecipient.setIsDeleted(false);
        allRecipients.add(senderRecipient);

        // TO recipients - pass currentUser (sender) to expandGroups
        if (request.getTo() != null) {
            allRecipients.addAll(createRecipients(savedMail, request.getTo(), "TO", sender));
        }

        // CC recipients
        if (request.getCc() != null) {
            allRecipients.addAll(createRecipients(savedMail, request.getCc(), "CC", sender));
        }

        // BCC recipients
        if (request.getBcc() != null) {
            allRecipients.addAll(createRecipients(savedMail, request.getBcc(), "BCC", sender));
        }

        recipientRepository.saveAll(allRecipients);
        savedMail.setRecipients(allRecipients);

        // Handle attachments
        if (files != null && !files.isEmpty()) {
            List<MailAttachment> attachments = saveAttachments(savedMail, files);
            attachmentRepository.saveAll(attachments);
            savedMail.setAttachments(attachments);
        }

        // If this was sent from a draft, delete the draft
        if (request.getDraftId() != null) {
            draftRepository.deleteById(request.getDraftId());
        }

        log.info("Mail sent successfully with ID: {}", savedMail.getId());

        return mailMapper.toResponse(savedMail, sender.getEmail());
    }


    private List<MailRecipient> createRecipients(Mail mail, List<String> emails, String type, User currentUser) {
        List<String> expandedEmails = expandGroups(emails, currentUser);
        List<MailRecipient> recipients = new ArrayList<>();

        // First, add the group identifiers themselves
        for (String email : emails) {
            if (email != null && email.startsWith("GROUP:")) {
                MailRecipient groupRecipient = new MailRecipient();
                groupRecipient.setMail(mail);
                groupRecipient.setEmail(email.trim()); // Keep the GROUP: prefix
                groupRecipient.setType(type);
                groupRecipient.setIsRead(false);
                groupRecipient.setIsStarred(false);
                groupRecipient.setIsArchived(false);
                groupRecipient.setIsDeleted(false);
                recipients.add(groupRecipient);
            }
        }

        // Then add all expanded individual emails
        for (String email : expandedEmails) {
            // Skip if this email is already added as a group recipient
            if (recipients.stream().anyMatch(r -> r.getEmail().equals(email))) {
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

            userRepository.findByEmail(email.trim()).ifPresent(u -> {
                recipient.setName(u.getFirstName() + " " + u.getLastName());
                recipient.setUser(u);
            });

            recipients.add(recipient);
        }

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
            Mail mail = mailRepository.findById(mailId)
                    .orElseThrow(() -> new RuntimeException("Mail not found: " + mailId));

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
                    response.setTo(objectMapper.readValue(draft.getToRecipients(), new TypeReference<List<String>>() {
                    }));
                }
                if (draft.getCcRecipients() != null) {
                    response.setCc(objectMapper.readValue(draft.getCcRecipients(), new TypeReference<List<String>>() {
                    }));
                }
                if (draft.getBccRecipients() != null) {
                    response.setBcc(objectMapper.readValue(draft.getBccRecipients(), new TypeReference<List<String>>() {
                    }));
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

        // NEW: Group mail statistics
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
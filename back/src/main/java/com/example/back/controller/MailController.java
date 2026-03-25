// MailController.java
package com.example.back.controller;

import com.example.back.entity.MailDraft;
import com.example.back.entity.MailFolder;
import com.example.back.entity.User;
import com.example.back.payload.request.MailActionRequest;
import com.example.back.payload.request.MailDraftRequest;
import com.example.back.payload.request.MailRequest;
import com.example.back.payload.response.*;
import com.example.back.repository.MailAttachmentRepository;
import com.example.back.repository.MailFolderRepository;
import com.example.back.repository.UserRepository;
import com.example.back.service.MailFolderService;
import com.example.back.service.MailService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/mails")
@Slf4j
public class MailController {

    @Autowired
    private MailService mailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailAttachmentRepository attachmentRepository;

    @Autowired
    private MailFolderRepository folderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MailFolderService folderService;



    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private User getCurrentUser() {
        String username = getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    // ============= SEND MAIL =============

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> sendMail(
            @RequestPart("request") @Valid MailRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        try {
            User currentUser = getCurrentUser();
            MailResponse response = mailService.sendMail(request, currentUser, attachments);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Email sent successfully");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error sending mail: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to send email: " + e.getMessage()));
        }
    }

    // ============= SAVE DRAFT =============


    @PostMapping(value = "/drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> saveDraft(
            @RequestPart("request") MailDraftRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        try {
            User currentUser = getCurrentUser();
            MailDraft savedDraft = mailService.saveDraft(request, currentUser, attachments);

            // Create a simplified response to avoid recursion
            Map<String, Object> draftResponse = new HashMap<>();
            draftResponse.put("id", savedDraft.getId());
            draftResponse.put("subject", savedDraft.getSubject());
            draftResponse.put("content", savedDraft.getContent());
            draftResponse.put("lastSavedAt", savedDraft.getLastSavedAt());

            // Parse recipients
            try {
                if (savedDraft.getToRecipients() != null) {
                    draftResponse.put("to", objectMapper.readValue(savedDraft.getToRecipients(), new TypeReference<List<String>>() {}));
                }
                if (savedDraft.getCcRecipients() != null) {
                    draftResponse.put("cc", objectMapper.readValue(savedDraft.getCcRecipients(), new TypeReference<List<String>>() {}));
                }
                if (savedDraft.getBccRecipients() != null) {
                    draftResponse.put("bcc", objectMapper.readValue(savedDraft.getBccRecipients(), new TypeReference<List<String>>() {}));
                }
            } catch (Exception e) {
                log.error("Error parsing recipients", e);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Draft saved successfully");
            response.put("data", draftResponse);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error saving draft: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to save draft: " + e.getMessage()));
        }
    }
    // ============= GET DRAFTS =============



    @GetMapping("/drafts")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getDrafts() {
        try {
            User currentUser = getCurrentUser();
            List<MailDraftResponse> drafts = mailService.getDraftsWithDetails(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", drafts);
            response.put("count", drafts.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching drafts: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch drafts"));
        }
    }

    @GetMapping("/drafts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getDraftById(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            MailDraft draft = mailService.getDraftById(id, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", draft);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching draft: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping(value = "/drafts/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> updateDraft(
            @PathVariable Long id,
            @RequestPart("request") MailDraftRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        try {
            User currentUser = getCurrentUser();
            MailDraft updatedDraft = mailService.updateDraft(id, request, currentUser, attachments);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Draft updated successfully");
            response.put("data", updatedDraft);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating draft: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to update draft: " + e.getMessage()));
        }
    }

    @DeleteMapping("/drafts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> deleteDraft(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            mailService.deleteDraft(id, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Draft deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting draft: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // ============= GET INBOX =============

    @GetMapping("/inbox")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            Page<MailResponse> mails = mailService.getInbox(currentUser.getEmail(), page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mails.getContent());
            response.put("totalPages", mails.getTotalPages());
            response.put("totalElements", mails.getTotalElements());
            response.put("currentPage", mails.getNumber());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching inbox: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch inbox"));
        }
    }

    // ============= GET SENT =============

    @GetMapping("/sent")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getSent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            Page<MailResponse> mails = mailService.getSent(currentUser.getEmail(), page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mails.getContent());
            response.put("totalPages", mails.getTotalPages());
            response.put("totalElements", mails.getTotalElements());
            response.put("currentPage", mails.getNumber());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching sent: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch sent"));
        }
    }

    // ============= GET STARRED =============

    @GetMapping("/starred")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getStarred(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            Page<MailResponse> mails = mailService.getStarred(currentUser.getEmail(), page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mails.getContent());
            response.put("totalPages", mails.getTotalPages());
            response.put("totalElements", mails.getTotalElements());
            response.put("currentPage", mails.getNumber());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching starred: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch starred"));
        }
    }

    // ============= GET ARCHIVED =============

    @GetMapping("/archived")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getArchived(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            Page<MailResponse> mails = mailService.getArchived(currentUser.getEmail(), page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mails.getContent());
            response.put("totalPages", mails.getTotalPages());
            response.put("totalElements", mails.getTotalElements());
            response.put("currentPage", mails.getNumber());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching archived: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch archived"));
        }
    }

    // Add this method to MailController.java
    @GetMapping("/trash")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getTrash(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            Page<MailResponse> mails = mailService.getTrash(currentUser.getEmail(), page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mails.getContent());
            response.put("totalPages", mails.getTotalPages());
            response.put("totalElements", mails.getTotalElements());
            response.put("currentPage", mails.getNumber());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching trash: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch trash"));
        }
    }


    // ============= GET MAIL BY ID =============

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getMailById(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            MailResponse mail = mailService.getMailById(id, currentUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mail);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching mail: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // ============= SEARCH MAILS =============

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> searchMails(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            Page<MailResponse> mails = mailService.searchMails(currentUser.getEmail(), q, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mails.getContent());
            response.put("totalPages", mails.getTotalPages());
            response.put("totalElements", mails.getTotalElements());
            response.put("currentPage", mails.getNumber());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching mails: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to search mails"));
        }
    }

    // ============= BATCH ACTIONS =============

    @PostMapping("/actions")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> performBatchAction(@Valid @RequestBody MailActionRequest request) {
        try {
            User currentUser = getCurrentUser();
            mailService.performBatchAction(request, currentUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Action performed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error performing batch action: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // ============= INDIVIDUAL ACTIONS =============

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            mailService.markAsRead(id, currentUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Marked as read");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error marking as read: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/unread")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> markAsUnread(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            mailService.markAsUnread(id, currentUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Marked as unread");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error marking as unread: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/star")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> toggleStar(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            MailResponse mail = mailService.getMailById(id, currentUser.getEmail());
            mailService.setStarred(id, currentUser.getEmail(), !mail.getIsStarred());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Star toggled");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error toggling star: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> toggleArchive(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            MailResponse mail = mailService.getMailById(id, currentUser.getEmail());
            mailService.setArchived(id, currentUser.getEmail(), !mail.getIsArchived());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Archive toggled");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error toggling archive: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> deleteMail(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            mailService.moveToTrash(id, currentUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Moved to trash");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting mail: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> restoreMail(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            mailService.restoreFromTrash(id, currentUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Restored from trash");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error restoring mail: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // ============= GET STATISTICS =============

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getStats() {
        try {
            User currentUser = getCurrentUser();
            MailStatsResponse stats = mailService.getStats(currentUser.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching stats: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch stats"));
        }
    }

    // ============= DOWNLOAD ATTACHMENT =============

    @GetMapping("/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            var attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment not found"));

            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(attachment.getFileType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error downloading attachment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }




    @GetMapping("/folders")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> getFolders() {
        try {
            User currentUser = getCurrentUser();

            // Create default system folders if they don't exist
            createDefaultFoldersIfNotExist(currentUser);

            List<MailFolder> folders = folderRepository.findByUserAndIsSystemFalseOrderByNameAsc(currentUser);

            // Add system folders to the list
            List<MailFolder> systemFolders = folderRepository.findByUserAndIsSystemTrueOrderByOrderIndexAsc(currentUser);

            List<MailFolderResponse> folderResponses = new ArrayList<>();

            // Add system folders first
            for (MailFolder folder : systemFolders) {
                MailFolderResponse response = new MailFolderResponse();
                response.setId(folder.getId());
                response.setName(folder.getName());
                response.setDescription(folder.getDescription());
                response.setColor(folder.getColor());
                response.setIsSystem(folder.getIsSystem());

                // Count mails in this folder (you'll need to implement this)
                response.setMailCount(0);
                response.setUnreadCount(0);

                folderResponses.add(response);
            }

            // Add custom folders
            for (MailFolder folder : folders) {
                MailFolderResponse response = new MailFolderResponse();
                response.setId(folder.getId());
                response.setName(folder.getName());
                response.setDescription(folder.getDescription());
                response.setColor(folder.getColor());
                response.setIsSystem(folder.getIsSystem());

                // Count mails in this folder
                response.setMailCount(0);
                response.setUnreadCount(0);

                folderResponses.add(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", folderResponses);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching folders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch folders"));
        }
    }

    private void createDefaultFoldersIfNotExist(User user) {
        String[] systemFolders = {"INBOX", "SENT", "DRAFTS", "STARRED", "ARCHIVE", "TRASH"};
        int index = 0;

        for (String folderName : systemFolders) {
            Optional<MailFolder> existingFolder = folderRepository.findByUserAndNameAndIsSystemTrue(user, folderName);
            if (existingFolder.isEmpty()) {
                MailFolder folder = new MailFolder();
                folder.setUser(user);
                folder.setName(folderName);
                folder.setIsSystem(true);
                folder.setOrderIndex(index++);
                folderRepository.save(folder);
            }
        }
    }

// ============= CREATE FOLDER =============

    @PostMapping("/folders")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> createFolder(@RequestBody Map<String, String> request) {
        try {
            User currentUser = getCurrentUser();
            String name = request.get("name");
            String color = request.get("color");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Folder name is required"));
            }

            MailFolder folder = new MailFolder();
            folder.setUser(currentUser);
            folder.setName(name);
            folder.setColor(color);
            folder.setIsSystem(false);

            MailFolder savedFolder = folderRepository.save(folder);

            MailFolderResponse response = new MailFolderResponse();
            response.setId(savedFolder.getId());
            response.setName(savedFolder.getName());
            response.setColor(savedFolder.getColor());
            response.setIsSystem(savedFolder.getIsSystem());
            response.setMailCount(0);
            response.setUnreadCount(0);

            Map<String, Object> apiResponse = new HashMap<>();
            apiResponse.put("success", true);
            apiResponse.put("message", "Folder created successfully");
            apiResponse.put("data", response);
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Error creating folder: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to create folder"));
        }
    }

// ============= DELETE FOLDER =============

    @DeleteMapping("/folders/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_PROJET', 'COMMERCIAL_METIER', 'DECIDEUR')")
    public ResponseEntity<?> deleteFolder(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            MailFolder folder = folderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            if (!folder.getUser().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(createErrorResponse("Access denied"));
            }

            if (folder.getIsSystem()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Cannot delete system folder"));
            }

            folderRepository.delete(folder);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Folder deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting folder: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to delete folder"));
        }
    }


    // Add these to MailController.java

    @GetMapping("/groups/{groupId}/mails")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getGroupMails(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = getCurrentUser();
            Page<MailResponse> mails = mailService.getGroupMails(groupId, currentUser.getEmail(), page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mails.getContent());
            response.put("totalPages", mails.getTotalPages());
            response.put("totalElements", mails.getTotalElements());
            response.put("currentPage", mails.getNumber());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching group mails: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch group mails"));
        }
    }


    @GetMapping("/with-unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getGroupsWithUnread() {
        try {
            User currentUser = getCurrentUser();
            List<MailGroupResponse> groups = folderService.getGroupsWithUnreadCounts(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", groups);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching groups with unread: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(createErrorResponse("Failed to fetch groups"));
        }
    }
}
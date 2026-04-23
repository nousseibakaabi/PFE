package com.example.back.payload.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MailResponse {

    private Long id;
    private String subject;
    private String content;
    private String senderEmail;
    private String senderName;
    private Long senderId;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private Boolean isRead;
    private Boolean isStarred;
    private Boolean isArchived;
    private Boolean hasAttachments;
    private String importance;

    private boolean isGroupMail;
    private String groupName;

    private List<RecipientResponse> recipients;
    private List<AttachmentResponse> attachments;

    private Long parentMailId;
    private Integer replyCount;

    @Data
    public static class RecipientResponse {
        private String email;
        private String name;
        private String type;
        private Boolean isRead;
        private LocalDateTime readAt;
    }

    @Data
    public static class AttachmentResponse {
        private Long id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String downloadUrl;
    }
}
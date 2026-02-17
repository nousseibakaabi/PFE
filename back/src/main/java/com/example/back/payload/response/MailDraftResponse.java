package com.example.back.payload.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class MailDraftResponse {
    private Long id;
    private String subject;
    private String content;
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private LocalDateTime lastSavedAt;
    private Boolean isSending;
    private List<Map<String, Object>> attachments;
}
// MailRequest.java
package com.example.back.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class MailRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Recipients are required")
    private List<String> to;

    private List<String> cc;

    private List<String> bcc;

    private String importance = "NORMAL";

    private Long parentMailId; // For reply/forward

    private List<MultipartFile> attachments;

    private Long draftId; // If sending from draft
}
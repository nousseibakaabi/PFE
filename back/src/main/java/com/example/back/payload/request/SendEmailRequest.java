package com.example.back.payload.request;

import lombok.Data;

@Data
public class SendEmailRequest {
    private Long factureId;
    private String to;
    private String subject;
    private String message;
    private String pdfBase64;
    private boolean isReminder;
}
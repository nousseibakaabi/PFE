package com.example.back.payload.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class MailDraftRequest {

    private String subject;

    private String content;

    private List<String> to;

    private List<String> cc;

    private List<String> bcc;

    private List<MultipartFile> attachments;
}
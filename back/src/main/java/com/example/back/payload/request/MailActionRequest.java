package com.example.back.payload.request;

import lombok.Data;

import java.util.List;

@Data
public class MailActionRequest {

    private List<Long> mailIds;

    private String action;

    private Long folderId;

    private String folderName;
}
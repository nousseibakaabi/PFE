package com.example.back.payload.response;

import lombok.Data;

@Data
public class MailFolderResponse {

    private Long id;
    private String name;
    private String description;
    private String color;
    private Boolean isSystem;
    private Integer mailCount;
    private Integer unreadCount;
    private String createdAt;
    private String updatedAt;
    private String folderType;
}
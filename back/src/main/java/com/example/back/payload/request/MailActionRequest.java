// MailActionRequest.java
package com.example.back.payload.request;

import lombok.Data;

import java.util.List;

@Data
public class MailActionRequest {

    private List<Long> mailIds;

    private String action; // READ, UNREAD, STAR, UNSTAR, ARCHIVE, UNARCHIVE, DELETE, RESTORE

    private Long folderId;

    private String folderName;
}
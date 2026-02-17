package com.example.back.payload.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MailGroupResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean isSystem;
    private Long ownerId;
    private String ownerName;
    private List<MemberInfo> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long unreadCount;
    private long mailCount; // Add this for total emails in group


    @Data
    public static class MemberInfo {
        private Long id;
        private String email;
        private String name;
        private String role;
    }
}
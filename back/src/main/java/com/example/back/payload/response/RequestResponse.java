// RequestResponse.java
package com.example.back.payload.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequestResponse {
    private Long id;
    private String requestType;
    private String status;

    // Requester info
    private Long requesterId;
    private String requesterName;
    private String requesterEmail;

    // Target user info
    private Long targetUserId;
    private String targetUserName;
    private String targetUserEmail;

    // Application info
    private Long applicationId;
    private String applicationName;
    private String applicationCode;

    // Convention info
    private Long conventionId;
    private String conventionReference;
    private String conventionLibelle;

    // Old convention info
    private Long oldConventionId;
    private String oldConventionReference;

    // Recommended chef info
    private Long recommendedChefId;
    private String recommendedChefName;
    private String recommendedChefEmail;

    private String reason;
    private String denialReason;
    private String recommendations;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String timeAgo;

    // For UI display
    private String statusColor;
    private String statusIcon;
    private String typeLabel;
}
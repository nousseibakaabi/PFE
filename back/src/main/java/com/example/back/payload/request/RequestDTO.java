// RequestDTO.java
package com.example.back.payload.request;

import lombok.Data;

@Data
public class RequestDTO {
    private Long id;
    private String requestType;
    private String status;
    private Long requesterId;
    private String requesterName;
    private Long targetUserId;
    private String targetUserName;
    private Long applicationId;
    private String applicationName;
    private String applicationCode;
    private Long conventionId;
    private String conventionReference;
    private Long oldConventionId;
    private String oldConventionReference;
    private Long recommendedChefId;
    private String recommendedChefName;
    private String reason;
    private String denialReason;
    private String recommendations;
    private String createdAt;
    private String processedAt;
}
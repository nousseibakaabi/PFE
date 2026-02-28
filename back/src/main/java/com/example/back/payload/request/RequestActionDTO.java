// RequestActionDTO.java
package com.example.back.payload.request;

import lombok.Data;

@Data
public class RequestActionDTO {
    private Long requestId;
    private String action; // APPROVE, DENY
    private String reason; // For denial
    private Long recommendedChefId; // For chef recommendations
    private String recommendations;
}
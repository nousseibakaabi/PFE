package com.example.back.payload.request;

import lombok.Data;

@Data
public class CreateReassignmentRequestDTO {
    private Long applicationId;
    private Long recommendedChefId;
    private String reason;
    private String recommendations;
}
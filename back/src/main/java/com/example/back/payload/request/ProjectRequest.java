// ProjectRequest.java
package com.example.back.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProjectRequest {

    @NotBlank(message = "Project code is required")
    private String code;

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    @NotNull(message = "Application is required")
    private Long applicationId;

    private Long chefDeProjetId;

    @NotBlank(message = "Client name is required")
    private String clientName;

    private String clientEmail;
    private String clientPhone;
    private String clientAddress;

    // Project dates
    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Progress (optional - can be calculated automatically)
    private Integer progress;

    // Budget
    private Double budget;

    private String status = "PLANIFIE";
}
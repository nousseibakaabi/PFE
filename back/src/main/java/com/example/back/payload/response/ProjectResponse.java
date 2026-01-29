// ProjectResponse.java
package com.example.back.payload.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String clientAddress;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer progress;
    private Double budget;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Calculated fields
    private Long daysRemaining;
    private Long totalDays;
    private Long daysElapsed;
    private Integer timeBasedProgress;
    private Boolean isDelayed;
    private String statusColor;
    private String progressColor;
    private String dateRange;
    private String timeRemainingString;

    // Related entity info
    private Long applicationId;
    private String applicationName;
    private String applicationCode;

    private Long chefDeProjetId;
    private String chefDeProjetUsername;
    private String chefDeProjetFullName;
    private String chefDeProjetEmail;

    // Statistics
    private int conventionsCount;
    private List<ConventionMiniResponse> recentConventions;

    @Data
    public static class ConventionMiniResponse {
        private Long id;
        private String referenceConvention;
        private String libelle;
        private String etat;
    }
}
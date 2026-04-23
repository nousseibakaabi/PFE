package com.example.back.payload.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApplicationResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long minUser;
    private Long maxUser;

    // Calculated fields
    private Long daysRemaining;
    private Long totalDays;
    private Long daysElapsed;
    private Integer timeBasedProgress;
    private Boolean isDelayed;
    private String statusColor;
    private String dateRange;
    private String timeRemainingString;

    // Related entity info
    private Long chefDeProjetId;
    private String chefDeProjetUsername;
    private String chefDeProjetFullName;
    private String chefDeProjetEmail;
    private String chefDeProjetProfileImage; // ADD THIS LINE

    // Statistics
    private int conventionsCount;
    private List<ConventionMiniResponse> recentConventions;


    private LocalDateTime terminatedAt;
    private String terminatedBy;
    private String terminationReason;
    private Long daysRemainingAtTermination;
    private String terminationInfo;
    private boolean terminatedEarly;
    private boolean terminatedOnTime;
    private boolean terminatedLate;

    private Long createdById;
    private String createdByUsername;
    private String createdByFullName;

    // Add these fields to ApplicationResponse
    private Boolean renewed;
    private LocalDateTime renewedAt;
    private String renewedBy;

    // Add these fields to ApplicationResponse class
    private Boolean archived;
    private LocalDateTime archivedAt;
    private String archivedBy;
    private String archivedReason;

    // Add getters and setters
    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }

    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }

    public void setArchivedBy(String archivedBy) { this.archivedBy = archivedBy; }

    public void setArchivedReason(String archivedReason) { this.archivedReason = archivedReason; }

    public void setRenewed(Boolean renewed) { this.renewed = renewed; }

    public void setRenewedAt(LocalDateTime renewedAt) { this.renewedAt = renewedAt; }

    public void setRenewedBy(String renewedBy) { this.renewedBy = renewedBy; }

    public LocalDateTime getTerminatedAt() { return terminatedAt; }
    public void setTerminatedAt(LocalDateTime terminatedAt) { this.terminatedAt = terminatedAt; }

    public String getTerminatedBy() { return terminatedBy; }
    public void setTerminatedBy(String terminatedBy) { this.terminatedBy = terminatedBy; }

    public void setTerminationReason(String terminationReason) { this.terminationReason = terminationReason; }

    public Long getDaysRemainingAtTermination() { return daysRemainingAtTermination; }
    public void setDaysRemainingAtTermination(Long daysRemainingAtTermination) {
        this.daysRemainingAtTermination = daysRemainingAtTermination;
    }

    public String getTerminationInfo() { return terminationInfo; }
    public void setTerminationInfo(String terminationInfo) { this.terminationInfo = terminationInfo; }

    public boolean isTerminatedEarly() { return terminatedEarly; }
    public void setTerminatedEarly(boolean terminatedEarly) { this.terminatedEarly = terminatedEarly; }

    public boolean isTerminatedOnTime() { return terminatedOnTime; }
    public void setTerminatedOnTime(boolean terminatedOnTime) { this.terminatedOnTime = terminatedOnTime; }

    public boolean isTerminatedLate() { return terminatedLate; }
    public void setTerminatedLate(boolean terminatedLate) { this.terminatedLate = terminatedLate; }

    @Data
    public static class ConventionMiniResponse {
        private Long id;
        private String referenceConvention;
        private String libelle;
        private String etat;
    }
}
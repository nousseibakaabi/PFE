package com.example.back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    // Archiving fields
    @Column(name = "archived")
    private Boolean archived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archived_by")
    private String archivedBy;

    @Column(name = "archived_reason")
    private String archivedReason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chef_de_projet_id")
    private User chefDeProjet;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "client_phone")
    private String clientPhone;


    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;


    @Column(name = "user_min")
    private Long minUser;

    @Column(name = "user_max")
    private Long maxUser;


    // Status
    @Column(nullable = false)
    private String status = "PLANIFIE"; // PLANIFIE, EN_COURS, TERMINE

    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relations
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Convention> conventions = new ArrayList<>();

    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    @Column(name = "terminated_by")
    private String terminatedBy;

    @Column(name = "termination_reason")
    private String terminationReason;


    // Add this field to Application.java
    @Column(name = "renewed")
    private Boolean renewed = false;

    @Column(name = "renewed_at")
    private LocalDateTime renewedAt;

    @Column(name = "renewed_by")
    private String renewedBy;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        updateStatusBasedOnDates();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStatusBasedOnDates();
    }

    // Helper methods
    public boolean isActive() {
        return "EN_COURS".equals(status);
    }

    public String getApplicationName() {
        return name != null ? name : "Non spécifié";
    }

    public String getChefProjetName() {
        return chefDeProjet != null ?
                (chefDeProjet.getFirstName() + " " + chefDeProjet.getLastName()) :
                "Non assigné";
    }

    public int getConventionsCount() {
        return conventions != null ? conventions.size() : 0;
    }


    // Get client info as string
    public String getClientInfo() {
        StringBuilder sb = new StringBuilder(clientName);
        if (clientEmail != null && !clientEmail.isEmpty()) {
            sb.append(" (").append(clientEmail).append(")");
        }
        return sb.toString();
    }



    // Calculate days remaining
    public Long getDaysRemaining() {
        if (dateFin == null || LocalDate.now().isAfter(dateFin)) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), dateFin);
    }

    // Calculate total days duration
    public Long getTotalDays() {
        if (dateDebut == null || dateFin == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
    }

    // Calculate days elapsed
    public Long getDaysElapsed() {
        if (dateDebut == null) {
            return 0L;
        }
        if (LocalDate.now().isBefore(dateDebut)) {
            return 0L;
        }
        if (dateFin != null && LocalDate.now().isAfter(dateFin)) {
            return getTotalDays();
        }
        return ChronoUnit.DAYS.between(dateDebut, LocalDate.now()) + 1;
    }

    // Calculate progress based on time
    public Integer getTimeBasedProgress() {
        if (dateDebut == null || dateFin == null) {
            return 0;
        }

        long totalDays = getTotalDays();
        long daysElapsed = getDaysElapsed();

        if (totalDays == 0) {
            return 0;
        }

        return (int) ((daysElapsed * 100) / totalDays);
    }


    public String getStatusColor() {
        return switch (status) {
            case "PLANIFIE" -> "yellow";
            case "EN_COURS" -> "blue";
            case "TERMINE" -> "green";
            default -> "gray";
        };
    }



    // Get formatted date range
    public String getDateRange() {
        if (dateDebut == null) return "Dates non définies";

        if (dateFin == null) {
            return "Depuis " + dateDebut.toString();
        }

        return dateDebut.toString() + " - " + dateFin.toString();
    }

    // Get time remaining as string
    public String getTimeRemainingString() {
        Long daysRemaining = getDaysRemaining();

        if (daysRemaining == null || daysRemaining == 0) {
            return "Terminé";
        }

        if (daysRemaining == 1) {
            return "1 jour restant";
        }

        return daysRemaining + " jours restants";
    }


        // NEW: Calculate days before due date when terminated
        public Long getDaysRemainingAtTermination() {
            if (terminatedAt == null || dateFin == null) {
                return null;
            }
            return ChronoUnit.DAYS.between(terminatedAt.toLocalDate(), dateFin);
        }

        // NEW: Get termination info for display
        public String getTerminationInfo() {
            if (terminatedAt == null) {
                return "Non terminée";
            }

            Long daysRemaining = getDaysRemainingAtTermination();
            String daysText = daysRemaining != null ?
                    (daysRemaining >= 0 ? daysRemaining + " jours avant échéance" :
                            Math.abs(daysRemaining) + " jours après échéance") :
                    "Date d'échéance inconnue";

            return String.format("Terminée le %s par %s (%s)",
                    terminatedAt.toLocalDate().toString(),
                    terminatedBy != null ? terminatedBy : "Système",
                    daysText);
        }

        // NEW: Check if terminated early
        public boolean isTerminatedEarly() {
            if (terminatedAt == null || dateFin == null) {
                return false;
            }
            return terminatedAt.toLocalDate().isBefore(dateFin);
        }

        // NEW: Check if terminated on time
        public boolean isTerminatedOnTime() {
            if (terminatedAt == null || dateFin == null) {
                return false;
            }
            return terminatedAt.toLocalDate().equals(dateFin);
        }

        // NEW: Check if terminated late
        public boolean isTerminatedLate() {
            if (terminatedAt == null || dateFin == null) {
                return false;
            }
            return terminatedAt.toLocalDate().isAfter(dateFin);
        }

        // Update the existing updateStatusBasedOnDates method
        public void updateStatusBasedOnDates() {
            if ("TERMINE".equals(this.status) && this.terminatedAt != null) {
                // Keep as terminated - don't auto-update
                return;
            }

            // If no start date, it's PLANIFIE
            if (dateDebut == null) {
                this.status = "PLANIFIE";
                return;
            }

            LocalDate today = LocalDate.now();

            // If there's an end date and today is after end date -> Auto TERMINE
            if (dateFin != null && today.isAfter(dateFin)) {
                this.status = "TERMINE";
                // Only set terminatedAt if not already set (auto-termination)
                if (this.terminatedAt == null) {
                    this.terminatedAt = LocalDateTime.now();
                    this.terminatedBy = "SYSTEM_AUTO";
                    this.terminationReason = "Date d'échéance dépassée";
                }
                return;
            }

            // If today is before start date -> PLANIFIE
            if (today.isBefore(dateDebut)) {
                this.status = "PLANIFIE";
            }
            // If today is between start and end date -> EN_COURS
            else {
                this.status = "EN_COURS";
            }
        }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Boolean getRenewed() { return renewed; }
    public void setRenewed(Boolean renewed) { this.renewed = renewed; }

    public LocalDateTime getRenewedAt() { return renewedAt; }
    public void setRenewedAt(LocalDateTime renewedAt) { this.renewedAt = renewedAt; }

    public String getRenewedBy() { return renewedBy; }
    public void setRenewedBy(String renewedBy) { this.renewedBy = renewedBy; }


}

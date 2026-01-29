// Project.java
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
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    // Link to Application (category)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    // Link to Chef de Projet (assigned manager)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chef_de_projet_id")
    private User chefDeProjet;

    // Client as regular string (not linked to Structure entity)
    @Column(name = "client_name", nullable = false)
    private String clientName;

    // Optional client details
    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "client_phone")
    private String clientPhone;

    @Column(name = "client_address")
    private String clientAddress;

    // Project dates (dd = date début, df = date fin)
    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    // Project progress (0-100)
    @Column(name = "progress")
    private Integer progress = 0;

    // Budget
    @Column(name = "budget")
    private Double budget;

    // Status
    @Column(nullable = false)
    private String status = "PLANIFIE"; // PLANIFIE, EN_COURS, TERMINE, SUSPENDU, ANNULE

    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relations
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Convention> conventions = new ArrayList<>();

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
        calculateProgress();
    }

    // Helper methods
    public boolean isActive() {
        return "EN_COURS".equals(status);
    }

    public String getApplicationName() {
        return application != null ? application.getName() : "Non spécifié";
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

    public void calculateProgress() {
        // If project is TERMINE, keep progress at 100%
        if ("TERMINE".equals(this.status)) {
            this.progress = 100;
            return;
        }

        // If project is SUSPENDU or ANNULE, don't change progress based on conventions
        if ("SUSPENDU".equals(this.status) || "ANNULE".equals(this.status)) {
            return; // Keep current progress
        }

        if (conventions == null || conventions.isEmpty()) {
            this.progress = 0;
            return;
        }

        // Calculate progress based on convention statuses
        long totalConventions = conventions.size();
        long termines = conventions.stream()
                .filter(c -> "TERMINE".equals(c.getEtat()))
                .count();

        if (termines == totalConventions) {
            this.progress = 100;
        } else if (totalConventions > 0) {
            this.progress = (int) ((termines * 100) / totalConventions);
        } else {
            this.progress = 0;
        }
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


    // Check if project is delayed
    public boolean isDelayed() {
        if (dateFin == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        boolean isPastDue = today.isAfter(dateFin);
        boolean isNotCompleted = !"TERMINE".equals(status) && progress < 100;

        return isPastDue && isNotCompleted;
    }

    // Get project status color for UI
    public String getStatusColor() {
        switch (status) {
            case "PLANIFIE":
                return "blue";
            case "EN_COURS":
                return "green";
            case "TERMINE":
                return "gray";
            case "SUSPENDU":
                return "orange";
            case "ANNULE":
                return "red";
            default:
                return "gray";
        }
    }

    // Get progress color based on percentage
    public String getProgressColor() {
        if (progress == null) return "gray";

        if (progress >= 90) return "success";
        if (progress >= 70) return "info";
        if (progress >= 50) return "warning";
        if (progress >= 30) return "primary";
        return "danger";
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


    // In Project.java - Update this method
    public void updateStatusBasedOnDates() {
        // If status is manually set to SUSPENDU or ANNULE, don't auto-update
        if ("SUSPENDU".equals(this.status) || "ANNULE".equals(this.status)) {
            return; // Keep manual status
        }

        // If no start date, it's PLANIFIE
        if (dateDebut == null) {
            this.status = "PLANIFIE";
            return;
        }

        LocalDate today = LocalDate.now();

        // Check if today is before start date
        if (today.isBefore(dateDebut)) {
            this.status = "PLANIFIE";
        }
        // Check if end date exists and today is after end date
        else if (dateFin != null && today.isAfter(dateFin)) {
            this.status = "TERMINE";
            // Auto-set progress to 100% if terminated
            if (this.progress != null && this.progress < 100) {
                this.progress = 100;
            }
        }
        // Otherwise, it's EN_COURS
        else {
            this.status = "EN_COURS";
        }
    }
}
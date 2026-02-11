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
    private String status = "PLANIFIE"; // PLANIFIE, EN_COURS, TERMINE, SUSPENDU, ANNULE

    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relations
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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


    // Get application status color for UI
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

    // In Application.java - Update this method
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

        // Otherwise, it's EN_COURS
        else {
            this.status = "EN_COURS";
        }
    }
}
package com.example.back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "factures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_facture", nullable = false, unique = true)
    private String numeroFacture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convention_id", nullable = false)
    private Convention convention;

    @Column(name = "date_facturation", nullable = false)
    private LocalDate dateFacturation;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "montant_ttc", precision = 15, scale = 2)
    private BigDecimal montantTTC;

    @Column(name = "tva", precision = 5, scale = 2)
    private BigDecimal tva = BigDecimal.valueOf(19.00);


    @Column(name = "statut_paiement")
    private String statutPaiement = "NON_PAYE";

    @Column(name = "date_paiement")
    private LocalDate datePaiement;



    @Column(name = "reference_paiement" )
    private String referencePaiement;

    @Column(name = "notes", length = 2000)
    private String notes;

    // New fields for archiving
    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (montantHT != null && tva != null) {
            BigDecimal tvaMontant = montantHT.multiply(tva).divide(BigDecimal.valueOf(100));
            montantTTC = montantHT.add(tvaMontant);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Method to archive invoice
    public void archive() {
        this.archived = true;
        this.archivedAt = LocalDateTime.now();
    }




    // In com.example.back.entity.Facture
    @Transient
    public Map<String, Object> getPaiementDetails() {
        Map<String, Object> details = new HashMap<>();

        if ("PAYE".equals(statutPaiement) && datePaiement != null && dateEcheance != null) {
            // Calculate time difference
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(datePaiement, dateEcheance);

            if (daysBetween > 0) {
                // Paid in advance
                details.put("type", "AVANCE");
                details.put("jours", daysBetween);
                details.put("message", formatDuration(daysBetween));
            } else if (daysBetween < 0) {
                // Paid late
                long daysLate = Math.abs(daysBetween);
                details.put("type", "RETARD");
                details.put("jours", daysLate);
                details.put("message", formatDuration(daysLate));
            } else {
                // Paid exactly on due date
                details.put("type", "PONCTUEL");
                details.put("jours", 0L);
                details.put("message", "Payé à la date d'échéance");
            }
        } else if (!"PAYE".equals(statutPaiement) && dateEcheance != null) {
            // Unpaid invoice
            LocalDate today = LocalDate.now();
            if (today.isBefore(dateEcheance)) {
                // Not yet due
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(today, dateEcheance);
                details.put("type", "EN_ATTENTE");
                details.put("jours", daysBetween);
                details.put("message", formatDuration(daysBetween));
            } else {
                // Overdue
                long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(dateEcheance, today);
                details.put("type", "EN_RETARD");
                details.put("jours", daysOverdue);
                details.put("message", formatDuration(daysOverdue));
            }
        }

        return details;
    }

    @Transient
    public String formatDuration(long days) {
        if (days == 0) return "0 jour";

        long years = days / 365;
        long months = (days % 365) / 30;
        long remainingDays = days % 30;

        List<String> parts = new ArrayList<>();

        if (years > 0) {
            parts.add(years + (years == 1 ? " an" : " ans"));
        }
        if (months > 0) {
            parts.add(months + (months == 1 ? " mois" : " mois"));
        }
        if (remainingDays > 0) {
            parts.add(remainingDays + (remainingDays == 1 ? " jour" : " jours"));
        }

        if (parts.isEmpty()) {
            return "0 jour";
        }

        return String.join(" et ", parts);
    }

    @Transient
    public String getStatutPaiementDetail() {
        Map<String, Object> details = getPaiementDetails();
        if (details.isEmpty()) return statutPaiement;

        String type = (String) details.get("type");
        String message = (String) details.get("message");

        switch (type) {
            case "AVANCE":
                return "Payé en avance de " + message;
            case "RETARD":
                return "Payé avec " + message + " de retard";
            case "PONCTUEL":
                return "Payé à la date d'échéance";
            case "EN_ATTENTE":
                return message + " restants";
            case "EN_RETARD":
                return message + " de retard";
            default:
                return statutPaiement;
        }
    }

    @Transient
    public String getStatutPaiementColor() {
        Map<String, Object> details = getPaiementDetails();
        if (details.isEmpty()) return "gray";

        String type = (String) details.get("type");

        switch (type) {
            case "AVANCE":
                return "emerald"; // Green for advance payment
            case "PONCTUEL":
                return "green"; // Darker green for on-time payment
            case "EN_ATTENTE":
                return "blue"; // Blue for pending payment
            case "RETARD":
            case "EN_RETARD":
                return "red"; // Red for late payment
            default:
                return "gray";
        }
    }

    @Transient
    public Integer getJoursRetard() {
        if ("PAYE".equals(statutPaiement) && datePaiement != null && dateEcheance != null) {
            // Calculate delay if paid after due date
            if (datePaiement.isAfter(dateEcheance)) {
                return (int) java.time.temporal.ChronoUnit.DAYS.between(dateEcheance, datePaiement);
            }
            return 0; // Paid on time
        }
        return null; // Not paid yet or missing data
    }

    @Transient
    public Integer getJoursRestants() {
        if (!"PAYE".equals(statutPaiement) && dateEcheance != null) {
            LocalDate today = LocalDate.now();
            if (today.isBefore(dateEcheance)) {
                // Days until due date
                return (int) java.time.temporal.ChronoUnit.DAYS.between(today, dateEcheance);
            } else {
                // Days of delay for unpaid invoices
                return (int) java.time.temporal.ChronoUnit.DAYS.between(dateEcheance, today) * -1;
            }
        }
        return null;
    }



    @Transient
    public boolean isEnRetard() {
        if ("PAYE".equals(statutPaiement)) {
            return false; // Paid invoices are not overdue
        }

        if (dateEcheance == null) {
            return false;
        }

        return LocalDate.now().isAfter(dateEcheance);
    }
}
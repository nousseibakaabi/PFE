package com.example.back.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conventions")
@Data
@NoArgsConstructor
public class Convention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceConvention;

    @Column(name = "reference_erp", unique = true, nullable = false)
    private String referenceERP;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private LocalDate dateDebut;

    private LocalDate dateFin;
    private LocalDate dateSignature;

    @ManyToOne
    @JoinColumn(name = "structure_interne_id", nullable = false)
    private Structure structureInterne;

    @ManyToOne
    @JoinColumn(name = "structure_externe_id", nullable = false)
    private Structure structureExterne;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    private ZoneGeographique zone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Project project;

    private java.math.BigDecimal montantTotal;
    private String periodicite;

    @Column(nullable = false)
    private String etat = "EN_ATTENTE"; // Default status: EN_ATTENTE

    // Archiving fields
    private Boolean archived = false;
    private LocalDateTime archivedAt;
    private String archivedBy;
    private String archivedReason;

    @OneToMany(mappedBy = "convention", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Facture> factures = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Get current user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                // Here you would fetch the User entity and set it
                // For now, we'll handle this in the service layer
            }
        }

        updateStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStatus();
    }


    /**
     * Update convention status based on dates and invoice payments
     * LOGIC:
     * 1. If archived → ARCHIVE
     * 2. If all invoices paid → TERMINE (highest priority after archived)
     * 3. If today < start date → EN_ATTENTE
     * 4. If has overdue unpaid invoices OR end date passed with unpaid invoices → EN_RETARD
     * 5. If today >= start date → EN_COURS (default)
     */
    public void updateStatus() {
        LocalDate today = LocalDate.now();

        // 1. If archived → ARCHIVE (cannot change from ARCHIVE)
        if (Boolean.TRUE.equals(archived)) {
            this.etat = "ARCHIVE";
            return;
        }

        // 2. Check if all invoices are paid → TERMINE (overrides date logic)
        boolean allInvoicesPaid = areAllInvoicesPaid();
        if (allInvoicesPaid) {
            this.etat = "TERMINE";
            return;
        }

        // 3. Check if convention hasn't started yet → EN_ATTENTE
        if (today.isBefore(dateDebut)) {
            this.etat = "EN_ATTENTE";
            return;
        }

        // 4. Check for overdue unpaid invoices
        boolean hasOverdueUnpaidInvoices = hasOverdueUnpaidInvoices();

        // 5. Check if end date is passed AND has unpaid invoices
        boolean endDatePassedWithUnpaidInvoices = false;
        if (dateFin != null && today.isAfter(dateFin)) {
            // If end date passed and not all invoices are paid
            if (!allInvoicesPaid) {
                endDatePassedWithUnpaidInvoices = true;
            }
        }

        // 6. If has overdue unpaid invoices OR end date passed with unpaid invoices → EN_RETARD
        if (hasOverdueUnpaidInvoices || endDatePassedWithUnpaidInvoices) {
            this.etat = "EN_RETARD";
            return;
        }

        // 7. Default: convention is in progress → EN_COURS
        // (today is on or after start date, no overdue invoices, end date not passed or passed but invoices paid)
        this.etat = "EN_COURS";
    }

    /**
     * Check if all invoices are paid
     */
    public boolean areAllInvoicesPaid() {
        if (factures == null || factures.isEmpty()) {
            return false; // No invoices yet, can't be TERMINE
        }

        // All invoices must be paid (status = "PAYE")
        boolean allPaid = factures.stream()
                .allMatch(facture -> "PAYE".equals(facture.getStatutPaiement()));

        return allPaid;
    }

    /**
     * Check if convention has any overdue unpaid invoices
     */
    public boolean hasOverdueUnpaidInvoices() {
        if (factures == null || factures.isEmpty()) {
            return false;
        }

        return factures.stream()
                .anyMatch(facture ->
                        facture.isEnRetard() && // Invoice is overdue
                                !"PAYE".equals(facture.getStatutPaiement()) // And not paid
                );
    }

    /**
     * Check if convention should transition from EN_ATTENTE to EN_COURS
     * Called by scheduled tasks
     */
    public boolean shouldTransitionToEnCours() {
        LocalDate today = LocalDate.now();

        // Should transition if:
        // 1. Not archived
        // 2. Currently EN_ATTENTE
        // 3. Today is on or after start date
        return !Boolean.TRUE.equals(archived) &&
                "EN_ATTENTE".equals(etat) &&
                !today.isBefore(dateDebut);
    }

    /**
     * Check if convention should transition from EN_COURS to EN_RETARD
     * Called by scheduled tasks
     */
    public boolean shouldTransitionToEnRetard() {
        LocalDate today = LocalDate.now();

        // Should transition if:
        // 1. Not archived
        // 2. Currently EN_COURS
        // 3. End date is not null and today is after end date
        // 4. Not all invoices are paid
        if (Boolean.TRUE.equals(archived) ||
                !"EN_COURS".equals(etat) ||
                dateFin == null ||
                today.isBefore(dateFin.plusDays(1))) { // Add 1 day to include end date
            return false;
        }

        // Check if not all invoices are paid
        return !areAllInvoicesPaid();
    }

    /**
     * Check if convention should be marked as TERMINE
     * Called by scheduled tasks
     */
    public boolean shouldBeTermine() {
        // Should be TERMINE if:
        // 1. Not archived
        // 2. Not already TERMINE
        // 3. All invoices are paid
        return !Boolean.TRUE.equals(archived) &&
                !"TERMINE".equals(etat) &&
                areAllInvoicesPaid();
    }

    /**
     * Get count of paid invoices
     */
    public long getPaidInvoicesCount() {
        if (factures == null) {
            return 0;
        }

        return factures.stream()
                .filter(facture -> "PAYE".equals(facture.getStatutPaiement()))
                .count();
    }

    /**
     * Get count of unpaid invoices
     */
    public long getUnpaidInvoicesCount() {
        if (factures == null) {
            return 0;
        }

        return factures.stream()
                .filter(facture -> !"PAYE".equals(facture.getStatutPaiement()))
                .count();
    }

    /**
     * Get count of overdue invoices
     */
    public long getOverdueInvoicesCount() {
        if (factures == null) {
            return 0;
        }

        return factures.stream()
                .filter(Facture::isEnRetard)
                .count();
    }

    /**
     * Get total amount of paid invoices
     */
    public java.math.BigDecimal getTotalPaidAmount() {
        if (factures == null) {
            return java.math.BigDecimal.ZERO;
        }

        return factures.stream()
                .filter(facture -> "PAYE".equals(facture.getStatutPaiement()))
                .map(Facture::getMontantTTC)
                .filter(amount -> amount != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    /**
     * Get total amount of unpaid invoices
     */
    public java.math.BigDecimal getTotalUnpaidAmount() {
        if (factures == null) {
            return java.math.BigDecimal.ZERO;
        }

        return factures.stream()
                .filter(facture -> !"PAYE".equals(facture.getStatutPaiement()))
                .map(Facture::getMontantTTC)
                .filter(amount -> amount != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    /**
     * Check if convention is currently active (not archived and not TERMINE)
     */
    public boolean isActive() {
        return !Boolean.TRUE.equals(archived) &&
                !"TERMINE".equals(etat) &&
                !"ARCHIVE".equals(etat);
    }

    /**
     * Check if convention is expiring soon (within 30 days)
     */
    public boolean isExpiringSoon() {
        if (dateFin == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);

        return !dateFin.isBefore(today) && !dateFin.isAfter(thirtyDaysFromNow);
    }

    /**
     * Get days until end date
     */
    public long getDaysUntilEnd() {
        if (dateFin == null) {
            return -1;
        }

        LocalDate today = LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(today, dateFin);
    }

    /**
     * Get days since start date
     */
    public long getDaysSinceStart() {
        LocalDate today = LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(dateDebut, today);
    }

    /**
     * Archive the convention
     */
    public void archive(String archivedBy, String reason) {
        this.archived = true;
        this.archivedAt = LocalDateTime.now();
        this.archivedBy = archivedBy;
        this.archivedReason = reason;
        this.etat = "ARCHIVE"; // Force status to ARCHIVE
    }

    /**
     * Restore the convention
     */
    public void restore() {
        this.archived = false;
        this.archivedAt = null;
        this.archivedBy = null;
        this.archivedReason = null;
        // Status will be updated by @PreUpdate when saved
    }

    /**
     * Generate invoices automatically based on periodicity
     * This should be called after the convention is created
     */
    public void generateInvoices() {
        if (montantTotal == null || periodicite == null || dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException("Missing required fields for invoice generation");
        }

        // Clear existing invoices (if any)
        if (factures != null) {
            factures.clear();
        } else {
            factures = new ArrayList<>();
        }

        // Calculate number of invoices based on periodicity
        int numberOfInvoices = calculateNumberOfInvoices();

        // Calculate invoice amount
        java.math.BigDecimal invoiceAmount = montantTotal.divide(
                java.math.BigDecimal.valueOf(numberOfInvoices),
                2,
                java.math.RoundingMode.HALF_UP
        );

        // Generate invoices
        LocalDate currentDate = dateDebut;

        for (int i = 1; i <= numberOfInvoices; i++) {
            Facture facture = new Facture();
            facture.setNumeroFacture(generateInvoiceNumber(i));
            facture.setConvention(this);
            facture.setDateFacturation(currentDate);
            facture.setDateEcheance(calculateDueDate(currentDate));
            facture.setMontantHT(invoiceAmount);
            facture.setTva(new java.math.BigDecimal("19.00"));
            facture.setStatutPaiement("NON_PAYE");
            facture.setNotes(String.format("Facture %d/%d pour la convention %s",
                    i, numberOfInvoices, referenceConvention));

            factures.add(facture);

            // Move to next period
            currentDate = getNextPeriodDate(currentDate);
        }
    }

    /**
     * Calculate number of invoices based on periodicity
     */
    private int calculateNumberOfInvoices() {
        if (periodicite == null || dateDebut == null || dateFin == null) {
            return 1;
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;

        switch (periodicite.toUpperCase()) {
            case "MENSUEL":
                return (int) Math.ceil(daysBetween / 30.0);
            case "TRIMESTRIEL":
                return (int) Math.ceil(daysBetween / 90.0);
            case "SEMESTRIEL":
                return (int) Math.ceil(daysBetween / 180.0);
            case "ANNUEL":
                return (int) Math.ceil(daysBetween / 365.0);
            default:
                return 1;
        }
    }

    /**
     * Calculate due date based on periodicite
     */
    private LocalDate calculateDueDate(LocalDate invoiceDate) {
        if (periodicite == null) {
            return invoiceDate.plusMonths(1);
        }

        switch (periodicite.toUpperCase()) {
            case "MENSUEL":
                return invoiceDate.plusMonths(1);
            case "TRIMESTRIEL":
                return invoiceDate.plusMonths(3);
            case "SEMESTRIEL":
                return invoiceDate.plusMonths(6);
            case "ANNUEL":
                return invoiceDate.plusYears(1);
            default:
                return invoiceDate.plusMonths(1);
        }
    }

    /**
     * Get next period date
     */
    private LocalDate getNextPeriodDate(LocalDate currentDate) {
        return calculateDueDate(currentDate);
    }

    /**
     * Generate invoice number
     */
    private String generateInvoiceNumber(int sequence) {
        return String.format("FACT-%s-%s-%03d",
                LocalDate.now().getYear(),
                referenceConvention,
                sequence);
    }

    /**
     * Validate convention data
     */
    public boolean isValid() {
        if (referenceConvention == null || referenceConvention.trim().isEmpty()) {
            return false;
        }

        if (libelle == null || libelle.trim().isEmpty()) {
            return false;
        }

        if (dateDebut == null) {
            return false;
        }

        if (structureInterne == null || structureExterne == null) {
            return false;
        }



        // If end date is provided, it must be after start date
        if (dateFin != null && !dateFin.isAfter(dateDebut)) {
            return false;
        }

        return true;
    }

    /**
     * Get status color for UI display
     */
    public String getStatusColor() {
        if (etat == null) {
            return "gray";
        }

        switch (etat) {
            case "EN_ATTENTE":
                return "yellow";
            case "EN_COURS":
                return "blue";
            case "EN_RETARD":
                return "red";
            case "TERMINE":
                return "green";
            case "ARCHIVE":
                return "gray";
            default:
                return "gray";
        }
    }

    /**
     * Get status label for UI display
     */
    public String getStatusLabel() {
        if (etat == null) {
            return "Non défini";
        }

        switch (etat) {
            case "EN_ATTENTE":
                return "En Attente";
            case "EN_COURS":
                return "En Cours";
            case "EN_RETARD":
                return "En Retard";
            case "TERMINE":
                return "Terminé";
            case "ARCHIVE":
                return "Archivé";
            default:
                return etat;
        }
    }

    @Override
    public String toString() {
        return "Convention{" +
                "id=" + id +
                ", referenceConvention='" + referenceConvention + '\'' +
                ", referenceERP='" + referenceERP + '\'' +
                ", libelle='" + libelle + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", etat='" + etat + '\'' +
                ", archived=" + archived +
                '}';
    }


    /**
     * Get application through project
     */
    public Application getApplication() {
        return project != null ? project.getApplication() : null;
    }

    /**
     * Get application name through project
     */
    public String getApplicationName() {
        return project != null ? project.getApplicationName() : null;
    }

    /**
     * Get client name from project
     */
    public String getClientNameFromProject() {
        return project != null ? project.getClientName() : null;
    }

    /**
     * Get chef de projet from project
     */
    public User getChefDeProjet() {
        return project != null ? project.getChefDeProjet() : null;
    }

    /**
     * Get project code
     */
    public String getProjectCode() {
        return project != null ? project.getCode() : null;
    }

    /**
     * Get project name
     */
    public String getProjectName() {
        return project != null ? project.getName() : null;
    }


}
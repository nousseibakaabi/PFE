package com.example.back.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
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

    @Column(name = "reference_erp", nullable = false , unique = false)
    private String referenceERP;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private LocalDate dateDebut;

    private LocalDate dateFin;
    private LocalDate dateSignature;

    @ManyToOne
    @JoinColumn(name = "structure_responsable_id", nullable = false)
    private Structure structureResponsable;

    @ManyToOne
    @JoinColumn(name = "structure_beneficiel_id", nullable = false)
    private Structure structureBeneficiel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id")
    private Application application;

    private java.math.BigDecimal montantTotal;
    private String periodicite;

    @Column(nullable = false)
    private String etat = "EN_ATTENTE";

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


    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "tva", precision = 5, scale = 2)
    private BigDecimal tva = BigDecimal.valueOf(19.00);

    @Column(name = "montant_ttc", precision = 15, scale = 2)
    private BigDecimal montantTTC;

    @Column(name = "nb_users")
    private Long nbUsers;


    @Column(name = "renewal_version")
    private Integer renewalVersion = 0;

    @OneToMany(mappedBy = "currentConvention", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OldConvention> oldVersions = new ArrayList<>();

    public Integer getRenewalVersion() { return renewalVersion; }
    public void setRenewalVersion(Integer renewalVersion) { this.renewalVersion = renewalVersion; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
            }
        }

        updateStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStatus();
    }



    public void updateStatus() {
        LocalDate today = LocalDate.now();

        if (Boolean.TRUE.equals(archived)) {
            this.etat = "ARCHIVE";
            return;
        }

        boolean allInvoicesPaid = areAllInvoicesPaid();
        if (allInvoicesPaid) {
            this.etat = "TERMINE";
            return;
        }

        if (dateDebut != null && today.isBefore(dateDebut)) {
            this.etat = "PLANIFIE";
            return;
        }

        this.etat = "EN COURS";
    }


    public boolean areAllInvoicesPaid() {
        if (factures == null || factures.isEmpty()) {
            return false;
        }
        return factures.stream()
                .allMatch(facture -> "PAYE".equals(facture.getStatutPaiement()));
    }

    public void archive(String archivedBy, String reason) {
        this.archived = true;
        this.archivedAt = LocalDateTime.now();
        this.archivedBy = archivedBy;
        this.archivedReason = reason;
        this.etat = "ARCHIVE"; // Force status to ARCHIVE
    }


    public void restore() {
        this.archived = false;
        this.archivedAt = null;
        this.archivedBy = null;
        this.archivedReason = null;
    }



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

        if (structureBeneficiel == null || structureResponsable == null) {
            return false;
        }

        return dateFin == null || dateFin.isAfter(dateDebut);
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

    public User getChefDeProjet() {
        return application != null ? application.getChefDeProjet() : null;
    }





}
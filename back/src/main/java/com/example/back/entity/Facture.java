package com.example.back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

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



    @Column(name = "reference_paiement")
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
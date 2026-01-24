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
    private String numeroFacture; // Numéro unique de facture

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convention_id", nullable = false)
    private Convention convention;

    @Column(name = "date_facturation", nullable = false)
    private LocalDate dateFacturation;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "montant_ht", precision = 10, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "tva", precision = 5, scale = 2)
    private BigDecimal tva = BigDecimal.valueOf(19.00); // TVA par défaut 19%

    @Column(name = "montant_ttc", precision = 10, scale = 2)
    private BigDecimal montantTTC;

    @Column(name = "statut_paiement")
    private String statutPaiement = "NON_PAYE"; // PAYE, NON_PAYE, EN_RETARD

    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    @Column(name = "mode_paiement")
    private String modePaiement; // Virement, Chèque, Espèces

    @Column(name = "reference_paiement")
    private String referencePaiement;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Calcul automatique du montant TTC si montant HT et TVA sont présents
        if (montantHT != null && tva != null) {
            BigDecimal tvaMontant = montantHT.multiply(tva).divide(BigDecimal.valueOf(100));
            montantTTC = montantHT.add(tvaMontant);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Méthode pour vérifier si la facture est en retard
    @Transient
    public boolean isEnRetard() {
        return "NON_PAYE".equals(statutPaiement) &&
                LocalDate.now().isAfter(dateEcheance);
    }
}
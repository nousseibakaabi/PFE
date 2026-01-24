package com.example.back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "conventions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Convention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, unique = true)
    private String reference; // Référence unique

    @Column(name = "libelle", nullable = false)
    private String libelle; // Libellé

    @Column(name = "date_debut")
    private LocalDate dateDebut; // Date début

    @Column(name = "date_fin")
    private LocalDate dateFin; // Date fin

    @Column(name = "date_signature")
    private LocalDate dateSignature; // Date signature

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id")
    private Structure structure; // Structure partenaire

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private ZoneGeographique gouvernorat; // Gouvernorat

    @Column(name = "montant_total", precision = 10, scale = 2)
    private BigDecimal montantTotal; // Montant total

    @Column(name = "modalites_paiement", length = 1000)
    private String modalitesPaiement; // Modalités de paiement

    @Column(name = "periodicite")
    private String periodicite; // Mensuel, Trimestriel, Semestriel, Annuel

    @Column(name = "etat")
    private String etat; // En cours, Terminé, Résilié, etc.

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
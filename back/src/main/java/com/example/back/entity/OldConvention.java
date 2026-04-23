// OldConvention.java
package com.example.back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "old_conventions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OldConvention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_convention_id", nullable = false)
    private Convention currentConvention;

    @Column(nullable = false)
    private String referenceConvention;

    private String referenceERP;
    private String libelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateSignature;

    @ManyToOne
    @JoinColumn(name = "structure_responsable_id")
    private Structure structureResponsable;

    @ManyToOne
    @JoinColumn(name = "structure_beneficiel_id")
    private Structure structureBeneficiel;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private Application application;

    private BigDecimal montantHT;
    private BigDecimal tva;
    private BigDecimal montantTTC;
    private Long nbUsers;
    private String periodicite;
    private String etat;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archived_by")
    private String archivedBy;

    @Column(name = "archived_reason")
    private String archivedReason;

    @Column(name = "renewal_version")
    private Integer renewalVersion; // To track multiple renewals

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
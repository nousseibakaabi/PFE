// OldFacture.java
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
@Table(name = "old_factures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OldFacture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_convention_id", nullable = false)
    private OldConvention oldConvention;

    @Column(nullable = false)
    private String numeroFacture;

    private LocalDate dateFacturation;
    private LocalDate dateEcheance;
    private BigDecimal montantHT;
    private BigDecimal tva;
    private BigDecimal montantTTC;
    private String statutPaiement;
    private LocalDate datePaiement;
    private String referencePaiement;
    private String notes;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}
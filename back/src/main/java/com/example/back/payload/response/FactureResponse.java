package com.example.back.payload.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FactureResponse {
    private Long id;
    private String numeroFacture;
    private LocalDate dateFacturation;
    private LocalDate dateEcheance;
    private BigDecimal montantHT;
    private BigDecimal montantTTC;
    private BigDecimal tva;
    private String statutPaiement;
    private LocalDate datePaiement;
    private String referencePaiement;
    private String notes;
    private Boolean archived;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean enRetard;

    // Convention reference (without circular dependency)
    private Long conventionId;
    private String conventionReference;
    private String conventionLibelle;

    // Additional useful fields for display
    private String structureInterneName;
    private String structureExterneName;
    private String zoneName;
    private String applicationName;
}
// FactureResponse.java - UPDATED
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

    // Convention reference
    private Long conventionId;
    private String conventionReference;
    private String conventionLibelle;

    // Project info (through convention)
    private Long projectId;
    private String projectCode;
    private String projectName;
    private String projectClientName;

    // Application info (through project)
    private Long applicationId;
    private String applicationName;
    private String applicationCode;

    // Chef de projet info (through project)
    private Long chefDeProjetId;
    private String chefDeProjetName;

    // Structure info (through convention)
    private String structureInterneName;
    private String structureExterneName;
    private String zoneName;


    private Integer joursRetard;
    private Integer joursRestants;
    private String statutPaiementDetail;
    private String statutPaiementColor;

    private String paiementType; // AVANCE, RETARD, PONCTUEL, EN_ATTENTE, EN_RETARD
    private String joursDetails; // Formatted duration like "1 an, 2 mois et 15 jours"
    private Long joursNumber; // Total number of days
}
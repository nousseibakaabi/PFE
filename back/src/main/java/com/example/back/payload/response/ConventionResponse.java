package com.example.back.payload.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConventionResponse {
    private Long id;
    private String referenceConvention;
    private String referenceERP;
    private String libelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateSignature;

    // NEW FINANCIAL FIELDS
    private BigDecimal montantHT;
    private BigDecimal tva;
    private BigDecimal montantTTC;
    private Long nbUsers;
    private String periodicite;

    private String etat;
    private Boolean archived;
    private LocalDateTime archivedAt;
    private String archivedBy;
    private String archivedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long structureResponsableId;
    private String structureResponsableName;
    private String structureResponsableCode;

    private Long structureBeneficielId;
    private String structureBeneficielName;
    private String structureBeneficielCode;

    private Long zoneId;
    private String zoneName;
    private String zoneCode;

    private Long applicationId;
    private String applicationName;
    private String applicationCode;
    private String applicationClientName;
    private Long minUser;
    private Long maxUser;

    private Long chefDeProjetId;
    private String chefDeProjetName;

    private List<FactureResponse> factures;
    private int totalFactures;
    private int facturesPayees;
    private int facturesNonPayees;
    private int facturesEnRetard;

    private Long createdById;
    private String createdByUsername;
    private String createdByFullName;
}
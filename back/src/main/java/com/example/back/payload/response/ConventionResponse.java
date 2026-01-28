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
    private BigDecimal montantTotal;
    private String periodicite;
    private String etat;
    private Boolean archived;
    private LocalDateTime archivedAt;
    private String archivedBy;
    private String archivedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Related entity basic info
    private Long structureInterneId;
    private String structureInterneName;
    private String structureInterneCode;

    private Long structureExterneId;
    private String structureExterneName;
    private String structureExterneCode;

    private Long zoneId;
    private String zoneName;
    private String zoneCode;

    private Long applicationId;
    private String applicationName;
    private String applicationCode;

    // Invoices
    private List<FactureResponse> factures;
    private int totalFactures;
    private int facturesPayees;
    private int facturesNonPayees;
    private int facturesEnRetard;
}
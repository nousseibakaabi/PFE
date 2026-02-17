package com.example.back.payload.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConventionHistoryData implements HistoryData {
    private Long id;
    private String referenceConvention;
    private String referenceERP;
    private String libelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateSignature;
    private BigDecimal montantHT;
    private BigDecimal tva;
    private BigDecimal montantTTC;
    private Long nbUsers;
    private String periodicite;
    private String etat;
    private Boolean archived;

    // Only store IDs to avoid recursion
    private Long applicationId;
    private String applicationName;
    private Long structureResponsableId;
    private String structureResponsableName;
    private Long structureBeneficielId;
    private String structureBeneficielName;
    private Long createdById;
    private String createdByName;
}

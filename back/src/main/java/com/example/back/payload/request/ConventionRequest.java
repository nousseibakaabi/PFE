package com.example.back.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ConventionRequest {

    @NotBlank(message = "Reference convention is required")
    private String referenceConvention;

    private String referenceERP;

    @NotBlank(message = "Libelle is required")
    private String libelle;

    @NotNull(message = "Start date is required")
    private LocalDate dateDebut;

    private LocalDate dateFin;
    private LocalDate dateSignature;

    @NotNull(message = "Internal structure is required")
    private Long structureInterneId;

    @NotNull(message = "External structure is required")
    private Long structureExterneId;

    @NotNull(message = "Zone is required")
    private Long zoneId;

    @NotNull(message = "Application is required")
    private Long applicationId;

    private BigDecimal montantTotal;

    private String periodicite; // MENSUEL, TRIMESTRIEL, SEMESTRIEL, ANNUEL

    // No etat field - it will be set automatically
}
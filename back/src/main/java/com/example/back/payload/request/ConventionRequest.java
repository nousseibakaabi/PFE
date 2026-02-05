package com.example.back.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ConventionRequest {

    @NotBlank(message = "Reference convention is required")
    @Pattern(regexp = "^CONV-\\d{4}-\\d{3}$", message = "Invalid reference format. Use CONV-YYYY-XXX")
    private String referenceConvention;

    @NotBlank(message = "Reference ERP is required")
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

    @NotNull(message = "Project is required")
    private Long projectId;

    private BigDecimal montantTotal;

    private String periodicite;

}
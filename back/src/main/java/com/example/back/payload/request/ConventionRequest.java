package com.example.back.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ConventionRequest {

    @NotBlank(message = "La référence est obligatoire")
    private String reference;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private LocalDate dateSignature;

    @NotNull(message = "La structure est obligatoire")
    private Long structureId;

    @NotNull(message = "Le gouvernorat est obligatoire")
    private Long gouvernoratId;

    private BigDecimal montantTotal;

    private String modalitesPaiement;

    private String periodicite;

    private String etat = "EN_COURS";
}
package com.example.back.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class FactureRequest {

    @NotNull(message = "La convention est obligatoire")
    private Long conventionId;

    private LocalDate dateFacturation;

    @NotNull(message = "La date d'échéance est obligatoire")
    private LocalDate dateEcheance;

    @NotNull(message = "Le montant HT est obligatoire")
    private BigDecimal montantHT;

    private BigDecimal tva;

    private String notes;
}
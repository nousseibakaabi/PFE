package com.example.back.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PaiementRequest {

    @NotNull(message = "La facture ID est obligatoire")
    private Long factureId;


    private String referencePaiement;

    private LocalDate datePaiement;
}
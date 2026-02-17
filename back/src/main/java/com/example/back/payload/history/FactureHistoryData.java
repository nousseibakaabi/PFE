package com.example.back.payload.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureHistoryData implements HistoryData {
    private Long id;
    private String numeroFacture;
    private LocalDate dateFacturation;
    private LocalDate dateEcheance;
    private BigDecimal montantHT;
    private BigDecimal tva;
    private BigDecimal montantTTC;
    private String statutPaiement;
    private LocalDate datePaiement;
    private String referencePaiement;
    private String notes;
    private Boolean archived;

    // Only store IDs to avoid recursion
    private Long conventionId;
    private String conventionReference;
}
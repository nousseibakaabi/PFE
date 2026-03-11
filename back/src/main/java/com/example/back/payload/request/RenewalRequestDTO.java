// RenewalRequestDTO.java
package com.example.back.payload.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RenewalRequestDTO {
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
    private Long structureResponsableId;


}
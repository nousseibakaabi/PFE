package com.example.back.payload.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationHistoryData implements HistoryData {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long minUser;
    private Long maxUser;
    private String status;
    private Long chefDeProjetId;
    private String chefDeProjetName;
}
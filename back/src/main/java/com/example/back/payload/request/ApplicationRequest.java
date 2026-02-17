package com.example.back.payload.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ApplicationRequest {

    @NotBlank(message = "Application code is required")
    @Pattern(regexp = "^APP-\\d{4}-\\d{3}$",
            message = "Invalid code format. Use APP-YYYY-XXX (ex: APP-2024-001)")
    private String code;

    @NotBlank(message = "Application name is required")
    private String name;

    private String description;

    private Long chefDeProjetId;

    @NotBlank(message = "Client name is required")
    private String clientName;

    private String clientEmail;
    private String clientPhone;

    // Application dates
    private LocalDate dateDebut;
    private LocalDate dateFin;



    private String status = "PLANIFIE";


    private Long minUser;

    private Long maxUser;
}
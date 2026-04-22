package com.example.back.payload.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ClientBilanRequest {
    private Long structureBeneficielId;
    private LocalDate dateStart;
    private LocalDate dateEnd;
    private Boolean includeArchived = false;
}
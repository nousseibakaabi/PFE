package com.example.back.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorRequest {
    @NotBlank
    private String code;

    private String backupCode;
}
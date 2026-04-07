package com.example.back.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "La question ne peut pas être vide")
    private String question;
}
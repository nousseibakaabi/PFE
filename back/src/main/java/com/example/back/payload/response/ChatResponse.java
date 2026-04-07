package com.example.back.payload.response;

import lombok.Data;

@Data
public class ChatResponse {
    private boolean success;
    private String question;
    private String answer;
}
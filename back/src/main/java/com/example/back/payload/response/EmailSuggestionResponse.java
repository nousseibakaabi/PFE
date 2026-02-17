package com.example.back.payload.response;

import lombok.Data;

@Data
public class EmailSuggestionResponse {
    private String email;
    private String name;
    private String type; // "USER", "GROUP"
    private Long id; // userId or groupId
    private String groupName; // if type is GROUP
    private String role; // if user
}
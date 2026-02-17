package com.example.back.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class MailGroupRequest {
    @NotBlank
    private String name;

    private String description;

    private List<Long> memberIds;
}
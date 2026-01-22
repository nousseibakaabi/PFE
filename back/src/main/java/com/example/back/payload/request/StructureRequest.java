// StructureRequest.java
package com.example.back.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StructureRequest extends NomenclatureRequest {

    private String address;

    private String phone;

    @Email(message = "Email should be valid")
    private String email;

    private String typeStructure;

    private String description;
}
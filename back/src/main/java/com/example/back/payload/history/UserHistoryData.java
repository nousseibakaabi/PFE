package com.example.back.payload.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserHistoryData implements HistoryData {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String department;
    private Boolean enabled;
    private Boolean lockedByAdmin;
    private Map<String, Object> roles;
}

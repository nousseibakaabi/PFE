package com.example.back.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponse {
    private Long id;
    private LocalDateTime timestamp;
    private String timeFormatted;
    private String dateFormatted;
    private String dateTimeFormatted;

    private String actionType;
    private String actionTypeLabel;

    private String entityType;
    private String entityTypeLabel;

    private Long entityId;
    private String entityCode;
    private String entityName;

    private Long userId;
    private String username;
    private String userFullName;
    private String userRole;

    private String description;

    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private boolean hasChanges;
    private int changedFieldsCount;

    private String ipAddress;
    private String userAgent;
}
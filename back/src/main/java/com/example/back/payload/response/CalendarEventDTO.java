package com.example.back.payload.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class CalendarEventDTO {
    private Long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private Boolean allDay;
    private String type; // "INVOICE" or "PROJECT"
    private String color;
    private Map<String, Object> extendedProps = new HashMap<>();

    private Long createdById;
    private String createdByName;
}
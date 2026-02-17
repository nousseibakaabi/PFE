// MailFilterRequest.java
package com.example.back.payload.request;

import lombok.Data;

@Data
public class MailFilterRequest {

    private String name;

    private String condition;

    private String action;

    private Boolean isActive = true;

    private Integer priority;
}
// MailSignatureRequest.java
package com.example.back.payload.request;

import lombok.Data;

@Data
public class MailSignatureRequest {

    private String name;

    private String content;

    private Boolean isDefault = false;

    private Boolean isHtml = true;
}
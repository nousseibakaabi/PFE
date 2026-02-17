package com.example.back.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mail_drafts")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MailDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String toRecipients; // JSON string

    private String ccRecipients; // JSON string

    private String bccRecipients; // JSON string

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<MailDraftAttachment> attachments = new ArrayList<>();

    private LocalDateTime lastSavedAt;

    private Boolean isSending = false;
}
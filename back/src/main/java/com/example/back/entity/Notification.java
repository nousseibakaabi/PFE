package com.example.back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String type; // INFO, WARNING, SUCCESS, DANGER

    @Column(name = "notification_type", nullable = false)
    private String notificationType; // FACTURE_DUE, FACTURE_OVERDUE, etc.

    @Column(name = "reference_id")
    private Long referenceId; // ID of the related entity (facture ID)

    @Column(name = "reference_type")
    private String referenceType; // FACTURE, CONVENTION, APPLICATION

    @Column(name = "reference_code")
    private String referenceCode; // Human-readable reference (facture number, convention ref)

    @Column(name = "days_until_due")
    private Integer daysUntilDue; // For due notifications: 5,4,3,2,1,0

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "email_sent")
    private Boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "sms_sent")
    private Boolean smsSent = false;

    @Column(name = "sms_sent_at")
    private LocalDateTime smsSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to mark as read
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    // Helper method to mark email as sent
    public void markEmailSent() {
        this.emailSent = true;
        this.emailSentAt = LocalDateTime.now();
        this.isSent = this.emailSent || this.smsSent;
        if (this.isSent && this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }

    // Helper method to mark SMS as sent
    public void markSmsSent() {
        this.smsSent = true;
        this.smsSentAt = LocalDateTime.now();
        this.isSent = this.emailSent || this.smsSent;
        if (this.isSent && this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }

    // Helper to check if notification is for a facture
    public boolean isFactureNotification() {
        return "FACTURE".equals(referenceType);
    }

    // Get days status message
    public String getDaysStatusMessage() {
        if (daysUntilDue == null) return "";

        if (daysUntilDue > 0) {
            return daysUntilDue == 1 ? "Due tomorrow" : "Due in " + daysUntilDue + " days";
        } else if (daysUntilDue == 0) {
            return "Due today";
        } else {
            return "Overdue by " + Math.abs(daysUntilDue) + " days";
        }
    }
}
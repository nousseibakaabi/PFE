package com.example.back.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_data")
@Getter
@Setter
public class TrainingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Invoice features
    private Double invoiceAmount;
    private Integer daysUntilDue;
    private Boolean isRecurring;

    // Client attributes
    private Long clientAge;
    private Integer totalConventions;
    private Double paymentOnTimeRate;
    private Double latePaymentRate;
    private Double advancePaymentRate;
    private Double averagePaymentDelay;
    private Integer previousLateCount;

    // Contract details
    private Long contractDuration;
    private Long nbUsers;

    // Seasonal factors
    private Boolean isEndOfMonth;
    private Boolean isEndOfQuarter;
    private Boolean isEndOfYear;

    // Outcome
    private Double riskScore;
    private Integer riskLevel;
    private Integer actualDelayDays;

    // Metadata
    private Boolean synthetic;
    private LocalDateTime createdAt;
    private LocalDateTime usedForTrainingAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }





}
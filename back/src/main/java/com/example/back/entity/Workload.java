package com.example.back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Entity
@Table(name = "workloads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Workload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chef_de_projet_id", nullable = false, unique = true)
    private User chefDeProjet;

    // Current workload metrics
    @Column(nullable = false)
    private Double currentWorkloadScore = 0.0; // 0-100 scale

    @Column(nullable = false)
    private Integer currentApplicationsCount = 0;

    @Column(nullable = false)
    private Double totalApplicationsValue = 0.0; // Sum of all app values

    @Column(nullable = false)
    private Long totalApplicationsDuration = 0L; // Sum of all app durations in days

    // Thresholds (percentage-based)
    @Column(nullable = false)
    private Double criticalThreshold = 75.0; // >75% = BLOCKED

    @Column(nullable = false)
    private Double highThreshold = 45.0; // 45-75% = OK

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @Column(name = "created_at", updatable = false)
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

    // Calculate workload percentage based on multiple factors
    public Double calculateWorkloadPercentage() {
        // Weight factors (configurable)
        double countWeight = 0.4;      // 40% weight for number of apps
        double valueWeight = 0.4;       // 40% weight for total value
        double durationWeight = 0.2;    // 20% weight for total duration

        // Calculate individual percentages
        double countPercentage = (currentApplicationsCount * 100.0) / 5; // 5 is max apps
        double valuePercentage = (totalApplicationsValue * 100.0) / 5000000.0; // 5M is max value
        double durationPercentage = (totalApplicationsDuration * 100.0) / 730; // 730 days is max duration

        // Cap at 100%
        countPercentage = Math.min(countPercentage, 100);
        valuePercentage = Math.min(valuePercentage, 100);
        durationPercentage = Math.min(durationPercentage, 100);

        // Calculate weighted average
        return (countPercentage * countWeight) +
                (valuePercentage * valueWeight) +
                (durationPercentage * durationWeight);
    }
}
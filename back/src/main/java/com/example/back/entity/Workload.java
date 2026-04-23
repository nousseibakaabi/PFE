package com.example.back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

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

}
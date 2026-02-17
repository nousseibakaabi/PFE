// MailFilter.java - Email filtering rules
package com.example.back.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mail_filters")
@Data
@NoArgsConstructor
public class MailFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String condition; // JSON string for conditions

    private String action; // JSON string for actions

    private Boolean isActive = true;

    private Integer priority;
}
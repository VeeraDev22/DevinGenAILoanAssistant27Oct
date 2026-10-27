package com.example.loanassistant.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Audit record of a single lending rule evaluation. The threshold in effect at
 * the time of evaluation is persisted so past decisions remain explainable
 * even after configuration changes.
 */
@Entity
@Table(name = "rule_evaluation")
@Getter
@Setter
public class RuleEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @Column(nullable = false, length = 128)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RuleOutcome outcome;

    @Column(nullable = false, length = 512)
    private String detail;

    @Column(precision = 19, scale = 4)
    private BigDecimal threshold;

    @Column(precision = 19, scale = 4)
    private BigDecimal actualValue;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

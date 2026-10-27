package com.example.loanassistant.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Advisory output produced by the AI assistant. This data is shown to loan
 * officers and used in customer communications, but it never determines the
 * approval decision — that is the exclusive job of the rules engine and,
 * when required, a human loan officer.
 */
@Entity
@Table(name = "ai_analysis_result")
@Getter
@Setter
public class AiAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication application;

    @Lob
    @Column
    private String summary;

    /** JSON array of missing/incomplete items flagged by the AI. */
    @Lob
    @Column
    private String missingInformation;

    /** JSON array of inconsistencies found between documents and the form. */
    @Lob
    @Column
    private String inconsistencies;

    /** JSON array of potential risk factors. */
    @Lob
    @Column
    private String riskFactors;

    /** JSON object of structured fields extracted from uploaded documents. */
    @Lob
    @Column
    private String extractedDataJson;

    @Column(length = 128)
    private String modelUsed;

    @Column(nullable = false)
    private boolean fallbackUsed;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

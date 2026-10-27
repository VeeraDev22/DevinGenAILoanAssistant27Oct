package com.example.loanassistant.service;

import java.util.Optional;

import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.Decision;
import com.example.loanassistant.domain.DecisionSource;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfficerReviewServiceTest {

    private LoanApplicationRepository applications;
    private ApplicationProcessingService processingService;
    private OfficerReviewService service;

    @BeforeEach
    void setUp() {
        applications = mock(LoanApplicationRepository.class);
        processingService = mock(ApplicationProcessingService.class);
        service = new OfficerReviewService(applications, processingService);
    }

    private LoanApplication pendingApp() {
        LoanApplication app = new LoanApplication();
        app.setId(7L);
        app.setStatus(ApplicationStatus.PENDING_MANUAL_REVIEW);
        return app;
    }

    @Test
    void officerApprovalUpdatesStatusAndRecordsDecision() {
        LoanApplication app = pendingApp();
        when(applications.findById(7L)).thenReturn(Optional.of(app));

        service.recordDecision(7L, Decision.APPROVED, "Documents verified", "officer");

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        verify(processingService).recordDecision(app, Decision.APPROVED,
                DecisionSource.LOAN_OFFICER, "officer", "Documents verified");
    }

    @Test
    void officerRejectionUpdatesStatus() {
        LoanApplication app = pendingApp();
        when(applications.findById(7L)).thenReturn(Optional.of(app));

        service.recordDecision(7L, Decision.REJECTED, "Insufficient income verification", "officer");

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(processingService).recordDecision(eq(app), eq(Decision.REJECTED),
                eq(DecisionSource.LOAN_OFFICER), eq("officer"), any());
    }

    @Test
    void rationaleIsRequired() {
        LoanApplication app = pendingApp();
        when(applications.findById(7L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.recordDecision(7L, Decision.APPROVED, " ", "officer"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotDecideApplicationNotPendingReview() {
        LoanApplication app = new LoanApplication();
        app.setId(7L);
        app.setStatus(ApplicationStatus.APPROVED);
        when(applications.findById(7L)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> service.recordDecision(7L, Decision.REJECTED, "changed mind", "officer"))
                .isInstanceOf(IllegalStateException.class);
    }
}

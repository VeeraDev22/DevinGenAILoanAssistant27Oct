package com.example.loanassistant.service;

import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.Decision;
import com.example.loanassistant.domain.DecisionSource;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * The human-in-the-loop path: a loan officer makes the final call on
 * applications the rules engine flagged for manual review. The officer's
 * username and rationale are recorded for audit; AI drafts the explanation.
 */
@Service
public class OfficerReviewService {

    private final LoanApplicationRepository applications;
    private final ApplicationProcessingService processingService;

    public OfficerReviewService(LoanApplicationRepository applications,
                                ApplicationProcessingService processingService) {
        this.applications = applications;
        this.processingService = processingService;
    }

    @Transactional
    public LoanApplication recordDecision(Long applicationId, Decision decision,
                                          String rationale, String officerUsername) {
        if (!StringUtils.hasText(rationale)) {
            throw new IllegalArgumentException("A rationale is required for an officer decision");
        }
        LoanApplication app = applications.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        if (app.getStatus() != ApplicationStatus.PENDING_MANUAL_REVIEW) {
            throw new IllegalStateException(
                    "Application " + applicationId + " is not pending manual review");
        }
        app.setStatus(decision == Decision.APPROVED
                ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED);
        processingService.recordDecision(app, decision, DecisionSource.LOAN_OFFICER,
                officerUsername, rationale);
        return app;
    }
}

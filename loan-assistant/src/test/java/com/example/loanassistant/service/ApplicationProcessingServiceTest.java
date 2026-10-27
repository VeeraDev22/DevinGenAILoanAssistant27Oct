package com.example.loanassistant.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.loanassistant.ai.AiAnalysis;
import com.example.loanassistant.ai.AiAnalysisService;
import com.example.loanassistant.config.LendingRuleProperties;
import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.Decision;
import com.example.loanassistant.domain.DecisionRecord;
import com.example.loanassistant.domain.DecisionSource;
import com.example.loanassistant.domain.EmploymentStatus;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.User;
import com.example.loanassistant.repository.AiAnalysisResultRepository;
import com.example.loanassistant.repository.DecisionRecordRepository;
import com.example.loanassistant.repository.LoanApplicationRepository;
import com.example.loanassistant.rules.CreditScoreRule;
import com.example.loanassistant.rules.DebtToIncomeRule;
import com.example.loanassistant.rules.EmploymentStabilityRule;
import com.example.loanassistant.rules.LendingRule;
import com.example.loanassistant.rules.LoanAmountRule;
import com.example.loanassistant.rules.LoanTermRule;
import com.example.loanassistant.rules.LoanToIncomeRule;
import com.example.loanassistant.rules.MinimumIncomeRule;
import com.example.loanassistant.rules.RulesEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The decision-control contract: AI output must never determine status.
 * Status comes only from the rules engine; AI is invoked for explanation
 * only after a decision exists.
 */
class ApplicationProcessingServiceTest {

    private LoanApplicationRepository applications;
    private AiAnalysisResultRepository aiResults;
    private DecisionRecordRepository decisions;
    private AiAnalysisService aiService;
    private NotificationService notificationService;
    private ApplicationProcessingService service;

    @BeforeEach
    void setUp() {
        applications = mock(LoanApplicationRepository.class);
        aiResults = mock(AiAnalysisResultRepository.class);
        decisions = mock(DecisionRecordRepository.class);
        aiService = mock(AiAnalysisService.class);
        notificationService = mock(NotificationService.class);

        LendingRuleProperties props = new LendingRuleProperties();
        List<LendingRule> rules = List.of(
                new CreditScoreRule(props), new DebtToIncomeRule(props),
                new MinimumIncomeRule(props), new LoanAmountRule(props),
                new LoanToIncomeRule(props), new EmploymentStabilityRule(props),
                new LoanTermRule(props));

        service = new ApplicationProcessingService(applications, aiResults, decisions,
                new RulesEngine(rules), aiService, notificationService, new ObjectMapper());
    }

    private LoanApplication app(int creditScore, BigDecimal income) {
        User customer = new User();
        customer.setUsername("customer");

        LoanApplication app = new LoanApplication();
        app.setId(42L);
        app.setCustomer(customer);
        app.setApplicantName("Test Applicant");
        app.setApplicantEmail("t@example.com");
        app.setAnnualIncome(income);
        app.setMonthlyDebtPayments(new BigDecimal("300"));
        app.setCreditScore(creditScore);
        app.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        app.setMonthsEmployed(60);
        app.setLoanAmount(new BigDecimal("20000"));
        app.setTermMonths(48);
        app.setLoanPurpose("Auto");
        app.setStatus(ApplicationStatus.SUBMITTED);
        return app;
    }

    private AiAnalysis anyAnalysis() {
        return new AiAnalysis("summary", List.of(), List.of(), List.of(),
                Map.of(), "claude-test", false);
    }

    @Test
    void aiCannotOverrideRulesDecision() {
        LoanApplication app = app(760, new BigDecimal("150000"));
        when(applications.findById(42L)).thenReturn(Optional.of(app));
        // AI "believes" the opposite; it must not matter.
        when(aiService.analyzeApplication(any(), any()))
                .thenReturn(new AiAnalysis("looks risky, should be denied",
                        List.of("everything"), List.of("fraud?"),
                        List.of("high risk"), Map.of(), "claude-test", false));
        when(aiService.explainDecision(any(), any(), any())).thenReturn("explanation");
        when(aiService.draftCustomerMessage(any(), any(), any())).thenReturn("message");

        service.process(42L);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(app.getRuleEvaluations()).hasSize(7);
        verify(decisions).save(any(DecisionRecord.class));
        verify(notificationService).notifyCustomer(eq(app), any(), eq("message"));
    }

    @Test
    void rejectedByRulesNotByAi() {
        LoanApplication app = app(500, new BigDecimal("150000"));
        when(applications.findById(42L)).thenReturn(Optional.of(app));
        when(aiService.analyzeApplication(any(), any())).thenReturn(anyAnalysis());
        when(aiService.explainDecision(any(), any(), any())).thenReturn("explanation");
        when(aiService.draftCustomerMessage(any(), any(), any())).thenReturn("message");

        service.process(42L);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        ArgumentCaptor<DecisionRecord> captor = ArgumentCaptor.forClass(DecisionRecord.class);
        verify(decisions).save(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo(Decision.REJECTED);
        assertThat(captor.getValue().getSource()).isEqualTo(DecisionSource.RULES_ENGINE);
        assertThat(captor.getValue().getDecidedBy()).isEqualTo("RULES_ENGINE");
    }

    @Test
    void reviewOutcomeRoutesToOfficerWithoutDecision() {
        LoanApplication app = app(660, new BigDecimal("150000"));
        when(applications.findById(42L)).thenReturn(Optional.of(app));
        when(aiService.analyzeApplication(any(), any())).thenReturn(anyAnalysis());

        service.process(42L);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING_MANUAL_REVIEW);
        verify(decisions, never()).save(any());
        // No explanation drafted — no decision exists yet.
        verify(aiService, never()).explainDecision(any(), any(), any());
        verify(notificationService).notifyCustomer(eq(app), any(), any());
    }

    @Test
    void explanationIsDraftedAfterDecisionExists() {
        LoanApplication app = app(760, new BigDecimal("150000"));
        when(applications.findById(42L)).thenReturn(Optional.of(app));
        when(aiService.analyzeApplication(any(), any())).thenReturn(anyAnalysis());
        when(aiService.explainDecision(any(), any(), any())).thenReturn("explanation");
        when(aiService.draftCustomerMessage(any(), any(), any())).thenReturn("message");

        service.process(42L);

        var order = inOrder(aiService);
        order.verify(aiService).analyzeApplication(any(), any());
        order.verify(aiService).explainDecision(any(), eq(Decision.APPROVED), any());
        order.verify(aiService).draftCustomerMessage(any(), eq(Decision.APPROVED), eq("explanation"));
    }

    @Test
    void alreadyProcessedApplicationIsNotReevaluated() {
        LoanApplication app = app(760, new BigDecimal("150000"));
        app.setStatus(ApplicationStatus.APPROVED);
        when(applications.findById(42L)).thenReturn(Optional.of(app));

        service.process(42L);

        verify(aiService, never()).analyzeApplication(any(), any());
        verify(decisions, never()).save(any());
    }
}

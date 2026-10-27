package com.example.loanassistant.rules;

import java.math.BigDecimal;
import java.util.List;

import com.example.loanassistant.config.LendingRuleProperties;
import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.EmploymentStatus;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.RuleOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RulesEngineTest {

    private LendingRuleProperties props;
    private RulesEngine engine;

    @BeforeEach
    void setUp() {
        props = new LendingRuleProperties();
        List<LendingRule> rules = List.of(
                new CreditScoreRule(props),
                new DebtToIncomeRule(props),
                new MinimumIncomeRule(props),
                new LoanAmountRule(props),
                new LoanToIncomeRule(props),
                new EmploymentStabilityRule(props),
                new LoanTermRule(props));
        engine = new RulesEngine(rules);
    }

    private LoanApplication strongApplicant() {
        LoanApplication app = new LoanApplication();
        app.setApplicantName("Strong Applicant");
        app.setApplicantEmail("strong@example.com");
        app.setAnnualIncome(new BigDecimal("120000"));
        app.setMonthlyDebtPayments(new BigDecimal("500"));
        app.setCreditScore(760);
        app.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        app.setEmployerName("Acme Corp");
        app.setMonthsEmployed(48);
        app.setLoanAmount(new BigDecimal("30000"));
        app.setTermMonths(36);
        app.setLoanPurpose("Home improvement");
        return app;
    }

    @Test
    void strongApplicationPassesAllRulesAndIsApproved() {
        List<RuleResult> results = engine.evaluate(strongApplicant());
        assertThat(results).hasSize(engine.ruleCount());
        assertThat(results).allMatch(r -> r.outcome() == RuleOutcome.PASS);
        assertThat(engine.aggregate(results)).isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    void creditScoreBelowMinimumIsRejected() {
        LoanApplication app = strongApplicant();
        app.setCreditScore(500);
        List<RuleResult> results = engine.evaluate(app);
        assertThat(results).anyMatch(r -> r.ruleName().equals("credit-score")
                && r.outcome() == RuleOutcome.FAIL);
        assertThat(engine.aggregate(results)).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void creditScoreInReviewBandTriggersManualReview() {
        LoanApplication app = strongApplicant();
        app.setCreditScore(660);
        List<RuleResult> results = engine.evaluate(app);
        assertThat(results).noneMatch(r -> r.outcome() == RuleOutcome.FAIL);
        assertThat(results).anyMatch(r -> r.ruleName().equals("credit-score")
                && r.outcome() == RuleOutcome.REVIEW);
        assertThat(engine.aggregate(results)).isEqualTo(ApplicationStatus.PENDING_MANUAL_REVIEW);
    }

    @Test
    void excessiveDtiIsRejected() {
        LoanApplication app = strongApplicant();
        app.setMonthlyDebtPayments(new BigDecimal("5000")); // DTI 0.50 > 0.43
        List<RuleResult> results = engine.evaluate(app);
        assertThat(results).anyMatch(r -> r.ruleName().equals("debt-to-income")
                && r.outcome() == RuleOutcome.FAIL);
        assertThat(engine.aggregate(results)).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void shortEmploymentHistoryTriggersReview() {
        LoanApplication app = strongApplicant();
        app.setMonthsEmployed(3);
        List<RuleResult> results = engine.evaluate(app);
        assertThat(results).anyMatch(r -> r.ruleName().equals("employment-stability")
                && r.outcome() == RuleOutcome.REVIEW);
        assertThat(engine.aggregate(results)).isEqualTo(ApplicationStatus.PENDING_MANUAL_REVIEW);
    }

    @Test
    void loanAmountAboveLimitIsRejected() {
        LoanApplication app = strongApplicant();
        app.setLoanAmount(new BigDecimal("400000"));
        List<RuleResult> results = engine.evaluate(app);
        assertThat(engine.aggregate(results)).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void failBeatsReviewWhenAggregating() {
        LoanApplication app = strongApplicant();
        app.setCreditScore(660);        // REVIEW
        app.setMonthsEmployed(48);
        app.setAnnualIncome(new BigDecimal("10000")); // FAIL min income
        List<RuleResult> results = engine.evaluate(app);
        assertThat(engine.aggregate(results)).isEqualTo(ApplicationStatus.REJECTED);
    }
}

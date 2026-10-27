package com.example.loanassistant;

import java.math.BigDecimal;
import java.util.List;

import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.EmploymentStatus;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.repository.AiAnalysisResultRepository;
import com.example.loanassistant.repository.DecisionRecordRepository;
import com.example.loanassistant.repository.RuleEvaluationRepository;
import com.example.loanassistant.service.ApplicationProcessingService;
import com.example.loanassistant.service.LoanApplicationService;
import com.example.loanassistant.web.dto.ApplicationForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test against a real MySQL 8 instance via
 * Testcontainers: Flyway migrations, JPA mapping validation, rule
 * persistence, decision audit trail and the AI-advisory boundary.
 */
@Testcontainers
@SpringBootTest(properties = {
        "anthropic.api-key=",
        "app.upload-dir=${java.io.tmpdir}/loan-it-uploads"
})
class LoanAssistantIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    LoanApplicationService applicationService;

    @Autowired
    ApplicationProcessingService processingService;

    @Autowired
    RuleEvaluationRepository ruleEvaluations;

    @Autowired
    DecisionRecordRepository decisions;

    @Autowired
    AiAnalysisResultRepository aiResults;

    private ApplicationForm form(int creditScore) {
        ApplicationForm form = new ApplicationForm();
        form.setApplicantName("Integration Tester");
        form.setApplicantEmail("it@example.com");
        form.setAnnualIncome(new BigDecimal("110000"));
        form.setMonthlyDebtPayments(new BigDecimal("400"));
        form.setCreditScore(creditScore);
        form.setEmploymentStatus(EmploymentStatus.EMPLOYED);
        form.setEmployerName("TestCo");
        form.setMonthsEmployed(36);
        form.setLoanAmount(new BigDecimal("25000"));
        form.setTermMonths(48);
        form.setLoanPurpose("Debt consolidation");
        return form;
    }

    @Test
    void submitAndProcessStrongApplicationEndToEnd() {
        LoanApplication app = applicationService.submit(form(780), List.of(), "customer");
        processingService.process(app.getId());

        LoanApplication reloaded = applicationService.getForCustomer(app.getId(), "customer");
        assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(reloaded.getRuleEvaluations()).isNotEmpty();
        assertThat(reloaded.getRuleEvaluations())
                .allMatch(e -> e.getThreshold() != null || e.getActualValue() != null);

        assertThat(decisions.findTopByApplicationIdOrderByCreatedAtDesc(app.getId()))
                .isPresent()
                .get()
                .satisfies(d -> assertThat(d.getCustomerExplanation()).isNotBlank());

        assertThat(aiResults.findByApplicationId(app.getId()))
                .isPresent()
                .get()
                .satisfies(r -> assertThat(r.isFallbackUsed()).isTrue());
    }

    @Test
    void weakApplicationIsRejectedByRules() {
        LoanApplication app = applicationService.submit(form(500), List.of(), "customer");
        processingService.process(app.getId());

        LoanApplication reloaded = applicationService.getForCustomer(app.getId(), "customer");
        assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }
}

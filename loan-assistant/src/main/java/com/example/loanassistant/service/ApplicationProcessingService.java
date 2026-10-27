package com.example.loanassistant.service;

import java.util.List;
import java.util.stream.Collectors;

import com.example.loanassistant.ai.AiAnalysis;
import com.example.loanassistant.ai.AiAnalysisService;
import com.example.loanassistant.domain.AiAnalysisResult;
import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.Decision;
import com.example.loanassistant.domain.DecisionRecord;
import com.example.loanassistant.domain.DecisionSource;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.RuleEvaluation;
import com.example.loanassistant.rules.RuleResult;
import com.example.loanassistant.rules.RulesEngine;
import com.example.loanassistant.repository.AiAnalysisResultRepository;
import com.example.loanassistant.repository.DecisionRecordRepository;
import com.example.loanassistant.repository.LoanApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates application processing. Order of operations is the safety
 * guarantee: AI advisory runs first and its output is stored for review, but
 * the status transition is computed exclusively by {@link RulesEngine} from
 * the applicant's declared financial data. AI is invoked again only AFTER a
 * decision exists, to draft its explanation.
 */
@Service
public class ApplicationProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationProcessingService.class);

    private final LoanApplicationRepository applications;
    private final AiAnalysisResultRepository aiResults;
    private final DecisionRecordRepository decisions;
    private final RulesEngine rulesEngine;
    private final AiAnalysisService aiService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ApplicationProcessingService(LoanApplicationRepository applications,
                                        AiAnalysisResultRepository aiResults,
                                        DecisionRecordRepository decisions,
                                        RulesEngine rulesEngine,
                                        AiAnalysisService aiService,
                                        NotificationService notificationService,
                                        ObjectMapper objectMapper) {
        this.applications = applications;
        this.aiResults = aiResults;
        this.decisions = decisions;
        this.rulesEngine = rulesEngine;
        this.aiService = aiService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LoanApplication process(Long applicationId) {
        LoanApplication app = applications.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        if (app.getStatus() != ApplicationStatus.SUBMITTED) {
            log.info("Application {} already processed (status {})", applicationId, app.getStatus());
            return app;
        }

        // Step 1 — AI advisory only: summary, extraction, gaps, risks.
        AiAnalysis analysis = aiService.analyzeApplication(app, app.getDocuments());
        persistAnalysis(app, analysis);

        // Step 2 — deterministic, auditable rules decide the outcome.
        List<RuleResult> results = rulesEngine.evaluate(app);
        results.forEach(r -> app.getRuleEvaluations().add(toEntity(app, r)));
        ApplicationStatus newStatus = rulesEngine.aggregate(results);
        app.setStatus(newStatus);
        log.info("Application {} evaluated: {} -> {}", applicationId, results, newStatus);

        // Step 3 — act on the decision.
        if (newStatus == ApplicationStatus.PENDING_MANUAL_REVIEW) {
            notificationService.notifyCustomer(app,
                    "Loan application under review",
                    "Your loan application #%d requires review by a loan officer. "
                            .formatted(app.getId())
                            + "You will be notified once a decision is made.");
        } else {
            Decision decision = newStatus == ApplicationStatus.APPROVED
                    ? Decision.APPROVED : Decision.REJECTED;
            recordDecision(app, decision, DecisionSource.RULES_ENGINE,
                    "RULES_ENGINE", summarize(results));
        }
        return app;
    }

    private void persistAnalysis(LoanApplication app, AiAnalysis analysis) {
        AiAnalysisResult result = aiResults.findByApplicationId(app.getId())
                .orElseGet(AiAnalysisResult::new);
        result.setApplication(app);
        result.setSummary(analysis.summary());
        result.setMissingInformation(toJson(analysis.missingInformation()));
        result.setInconsistencies(toJson(analysis.inconsistencies()));
        result.setRiskFactors(toJson(analysis.riskFactors()));
        result.setExtractedDataJson(toJson(analysis.extractedFields()));
        result.setModelUsed(analysis.modelUsed());
        result.setFallbackUsed(analysis.fallbackUsed());
        aiResults.save(result);
    }

    /** Shared by the rules path here and the officer path in OfficerReviewService. */
    DecisionRecord recordDecision(LoanApplication app, Decision decision,
                                  DecisionSource source, String decidedBy, String rationale) {
        // AI drafts the plain-language explanation of a decision it did not make.
        List<RuleEvaluation> evals = app.getRuleEvaluations();
        String explanation = aiService.explainDecision(app, decision, evals);
        String message = aiService.draftCustomerMessage(app, decision, explanation);

        DecisionRecord record = new DecisionRecord();
        record.setApplication(app);
        record.setDecision(decision);
        record.setSource(source);
        record.setDecidedBy(decidedBy);
        record.setRationale(rationale);
        record.setCustomerExplanation(explanation);
        decisions.save(record);
        app.getDecisions().add(record);

        notificationService.notifyCustomer(app,
                "Your loan application #%d has been %s".formatted(app.getId(), decision),
                message);
        return record;
    }

    private RuleEvaluation toEntity(LoanApplication app, RuleResult r) {
        RuleEvaluation e = new RuleEvaluation();
        e.setApplication(app);
        e.setRuleName(r.ruleName());
        e.setOutcome(r.outcome());
        e.setDetail(r.detail());
        e.setThreshold(r.threshold());
        e.setActualValue(r.actualValue());
        return e;
    }

    private String summarize(List<RuleResult> results) {
        return results.stream()
                .map(r -> "[%s] %s — %s".formatted(r.outcome(), r.ruleName(), r.detail()))
                .collect(Collectors.joining("\n"));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "null";
        }
    }
}

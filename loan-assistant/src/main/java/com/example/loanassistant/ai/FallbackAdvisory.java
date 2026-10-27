package com.example.loanassistant.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.loanassistant.domain.Decision;
import com.example.loanassistant.domain.EmploymentStatus;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.LoanDocument;
import com.example.loanassistant.domain.RuleEvaluation;
import com.example.loanassistant.domain.RuleOutcome;

/**
 * Deterministic, local advisory used when the Anthropic API key is not
 * configured or the API is unreachable. Keeps the application fully runnable
 * while preserving the same advisory-only contract.
 */
final class FallbackAdvisory {

    static final String MODEL = "local-fallback";

    private FallbackAdvisory() {
    }

    static AiAnalysis analyze(LoanApplication app, List<LoanDocument> documents) {
        String summary = """
                %s requested %s over %d months for "%s". Declared annual income %s, \
                monthly debt payments %s, credit score %d, employment status %s (%d months \
                with %s). %d document(s) attached.""".formatted(
                app.getApplicantName(), app.getLoanAmount(), app.getTermMonths(),
                app.getLoanPurpose(), app.getAnnualIncome(), app.getMonthlyDebtPayments(),
                app.getCreditScore(), app.getEmploymentStatus(), app.getMonthsEmployed(),
                app.getEmployerName() == null ? "no employer listed" : app.getEmployerName(),
                documents == null ? 0 : documents.size());

        List<String> missing = new ArrayList<>();
        if (documents == null || documents.isEmpty()) {
            missing.add("No supporting documents were uploaded (e.g., pay stubs, bank statements, ID).");
        }
        if (app.getEmploymentStatus() == EmploymentStatus.EMPLOYED
                && (app.getEmployerName() == null || app.getEmployerName().isBlank())) {
            missing.add("Employer name was not provided.");
        }

        List<String> risks = new ArrayList<>();
        BigDecimal income = app.getAnnualIncome();
        if (income != null && income.signum() > 0) {
            BigDecimal dti = app.getMonthlyDebtPayments().multiply(BigDecimal.valueOf(12))
                    .divide(income, 4, RoundingMode.HALF_UP);
            if (dti.compareTo(new BigDecimal("0.36")) > 0) {
                risks.add("Elevated debt-to-income ratio: " + dti);
            }
        }
        if (app.getCreditScore() < 700) {
            risks.add("Credit score below 700.");
        }
        if (app.getMonthsEmployed() < 12) {
            risks.add("Less than 12 months of employment history.");
        }

        Map<String, Object> extracted = new LinkedHashMap<>();
        if (documents != null) {
            for (LoanDocument doc : documents) {
                if (doc.getExtractedText() != null && !doc.getExtractedText().isBlank()) {
                    String text = doc.getExtractedText();
                    extracted.put(doc.getOriginalFilename(),
                            text.length() > 500 ? text.substring(0, 500) + "…" : text);
                }
            }
        }

        return new AiAnalysis(summary, missing, List.of(), risks, extracted, MODEL, true);
    }

    static String explain(Decision decision, List<RuleEvaluation> evaluations) {
        StringBuilder sb = new StringBuilder();
        sb.append(decision == Decision.APPROVED
                ? "Your loan application has been approved. "
                : "Your loan application was not approved. ");
        sb.append("Our standard lending criteria produced the following results:");
        for (RuleEvaluation e : evaluations) {
            sb.append("\n- ").append(e.getDetail());
            if (e.getOutcome() == RuleOutcome.REVIEW) {
                sb.append(" (flagged for review)");
            }
        }
        return sb.toString();
    }

    static String customerMessage(LoanApplication app, Decision decision, String explanation) {
        return """
                Dear %s,

                %s

                Application reference: #%d
                Loan amount requested: %s over %d months

                If you have questions about this decision or believe information was missed, \
                please contact our lending team and quote your application reference.

                Sincerely,
                Loan Processing Team""".formatted(
                app.getApplicantName(), explanation, app.getId(),
                app.getLoanAmount(), app.getTermMonths());
    }
}

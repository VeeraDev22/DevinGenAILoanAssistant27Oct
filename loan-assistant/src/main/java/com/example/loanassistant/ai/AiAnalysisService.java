package com.example.loanassistant.ai;

import java.util.List;

import com.example.loanassistant.domain.Decision;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.LoanDocument;
import com.example.loanassistant.domain.RuleEvaluation;

/**
 * The AI assistant's advisory interface. Implementations may summarize,
 * extract, flag inconsistencies and draft language — they are structurally
 * incapable of approving or declining an application.
 */
public interface AiAnalysisService {

    /** Summarizes the application, extracts document data, flags gaps and risks. */
    AiAnalysis analyzeApplication(LoanApplication application, List<LoanDocument> documents);

    /** Drafts a plain-language explanation of a decision already made by rules or an officer. */
    String explainDecision(LoanApplication application, Decision decision, List<RuleEvaluation> evaluations);

    /** Drafts the customer-facing message body announcing a final decision. */
    String draftCustomerMessage(LoanApplication application, Decision decision, String explanation);
}

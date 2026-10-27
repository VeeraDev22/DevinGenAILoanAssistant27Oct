package com.example.loanassistant.rules;

import java.util.List;

import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.RuleOutcome;
import org.springframework.stereotype.Service;

/**
 * Evaluates all configured lending rules and aggregates the results into an
 * application status. This is the sole authority for automated decisions:
 * any FAIL rejects, otherwise any REVIEW routes to a human loan officer,
 * and a clean sweep approves. AI output is never an input here.
 */
@Service
public class RulesEngine {

    private final List<LendingRule> rules;

    public RulesEngine(List<LendingRule> rules) {
        this.rules = rules;
    }

    public List<RuleResult> evaluate(LoanApplication application) {
        return rules.stream().map(rule -> rule.evaluate(application)).toList();
    }

    public ApplicationStatus aggregate(List<RuleResult> results) {
        if (results.stream().anyMatch(r -> r.outcome() == RuleOutcome.FAIL)) {
            return ApplicationStatus.REJECTED;
        }
        if (results.stream().anyMatch(r -> r.outcome() == RuleOutcome.REVIEW)) {
            return ApplicationStatus.PENDING_MANUAL_REVIEW;
        }
        return ApplicationStatus.APPROVED;
    }

    public int ruleCount() {
        return rules.size();
    }
}

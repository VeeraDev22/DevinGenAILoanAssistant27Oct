package com.example.loanassistant.rules;

import java.math.BigDecimal;

import com.example.loanassistant.domain.RuleOutcome;

/**
 * The outcome of a single lending-rule evaluation, including the values
 * needed to audit the decision later.
 */
public record RuleResult(
        String ruleName,
        RuleOutcome outcome,
        String detail,
        BigDecimal threshold,
        BigDecimal actualValue) {

    public static RuleResult pass(String ruleName, String detail, BigDecimal threshold, BigDecimal actual) {
        return new RuleResult(ruleName, RuleOutcome.PASS, detail, threshold, actual);
    }

    public static RuleResult review(String ruleName, String detail, BigDecimal threshold, BigDecimal actual) {
        return new RuleResult(ruleName, RuleOutcome.REVIEW, detail, threshold, actual);
    }

    public static RuleResult fail(String ruleName, String detail, BigDecimal threshold, BigDecimal actual) {
        return new RuleResult(ruleName, RuleOutcome.FAIL, detail, threshold, actual);
    }
}

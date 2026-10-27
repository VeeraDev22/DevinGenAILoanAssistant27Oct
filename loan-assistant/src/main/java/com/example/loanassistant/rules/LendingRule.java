package com.example.loanassistant.rules;

import com.example.loanassistant.domain.LoanApplication;

/**
 * A single configurable lending-policy rule. Rules evaluate only financial
 * data supplied on the application — never AI output and never protected
 * personal characteristics (which are not collected at all).
 */
public interface LendingRule {

    String name();

    RuleResult evaluate(LoanApplication application);
}

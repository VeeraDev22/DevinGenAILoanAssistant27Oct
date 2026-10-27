package com.example.loanassistant.rules;

import java.math.BigDecimal;

import com.example.loanassistant.config.LendingRuleProperties;
import com.example.loanassistant.domain.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class MinimumIncomeRule implements LendingRule {

    private final LendingRuleProperties props;

    public MinimumIncomeRule(LendingRuleProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "minimum-income";
    }

    @Override
    public RuleResult evaluate(LoanApplication app) {
        BigDecimal income = app.getAnnualIncome();
        if (income == null || income.compareTo(props.getMinAnnualIncome()) < 0) {
            return RuleResult.fail(name(),
                    "Annual income %s is below the minimum of %s".formatted(income, props.getMinAnnualIncome()),
                    props.getMinAnnualIncome(), income);
        }
        return RuleResult.pass(name(),
                "Annual income %s meets the minimum of %s".formatted(income, props.getMinAnnualIncome()),
                props.getMinAnnualIncome(), income);
    }
}

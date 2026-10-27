package com.example.loanassistant.rules;

import java.math.BigDecimal;

import com.example.loanassistant.config.LendingRuleProperties;
import com.example.loanassistant.domain.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class LoanAmountRule implements LendingRule {

    private final LendingRuleProperties props;

    public LoanAmountRule(LendingRuleProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "loan-amount-range";
    }

    @Override
    public RuleResult evaluate(LoanApplication app) {
        BigDecimal amount = app.getLoanAmount();
        if (amount == null
                || amount.compareTo(props.getMinLoanAmount()) < 0
                || amount.compareTo(props.getMaxLoanAmount()) > 0) {
            return RuleResult.fail(name(),
                    "Loan amount %s is outside the allowed range %s - %s"
                            .formatted(amount, props.getMinLoanAmount(), props.getMaxLoanAmount()),
                    props.getMaxLoanAmount(), amount);
        }
        return RuleResult.pass(name(),
                "Loan amount %s is within the allowed range %s - %s"
                        .formatted(amount, props.getMinLoanAmount(), props.getMaxLoanAmount()),
                props.getMaxLoanAmount(), amount);
    }
}

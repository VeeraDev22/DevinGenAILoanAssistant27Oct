package com.example.loanassistant.rules;

import java.math.BigDecimal;

import com.example.loanassistant.config.LendingRuleProperties;
import com.example.loanassistant.domain.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class LoanTermRule implements LendingRule {

    private final LendingRuleProperties props;

    public LoanTermRule(LendingRuleProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "loan-term";
    }

    @Override
    public RuleResult evaluate(LoanApplication app) {
        BigDecimal term = BigDecimal.valueOf(app.getTermMonths());
        if (app.getTermMonths() <= 0 || app.getTermMonths() > props.getMaxTermMonths()) {
            return RuleResult.fail(name(),
                    "Term %d months is outside the allowed range 1 - %d"
                            .formatted(app.getTermMonths(), props.getMaxTermMonths()),
                    BigDecimal.valueOf(props.getMaxTermMonths()), term);
        }
        return RuleResult.pass(name(),
                "Term %d months is within the maximum of %d"
                        .formatted(app.getTermMonths(), props.getMaxTermMonths()),
                BigDecimal.valueOf(props.getMaxTermMonths()), term);
    }
}

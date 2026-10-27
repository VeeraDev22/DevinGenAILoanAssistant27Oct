package com.example.loanassistant.rules;

import java.math.BigDecimal;

import com.example.loanassistant.config.LendingRuleProperties;
import com.example.loanassistant.domain.EmploymentStatus;
import com.example.loanassistant.domain.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class EmploymentStabilityRule implements LendingRule {

    private final LendingRuleProperties props;

    public EmploymentStabilityRule(LendingRuleProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "employment-stability";
    }

    @Override
    public RuleResult evaluate(LoanApplication app) {
        BigDecimal months = BigDecimal.valueOf(app.getMonthsEmployed());
        if (app.getEmploymentStatus() == EmploymentStatus.UNEMPLOYED) {
            return RuleResult.review(name(),
                    "Applicant is currently unemployed; income source needs officer verification",
                    BigDecimal.valueOf(props.getMinMonthsEmployed()), months);
        }
        if (app.getMonthsEmployed() < props.getMinMonthsEmployed()) {
            return RuleResult.review(name(),
                    "Only %d months of employment history; minimum for auto-approval is %d"
                            .formatted(app.getMonthsEmployed(), props.getMinMonthsEmployed()),
                    BigDecimal.valueOf(props.getMinMonthsEmployed()), months);
        }
        return RuleResult.pass(name(),
                "%d months of employment history meets the minimum of %d"
                        .formatted(app.getMonthsEmployed(), props.getMinMonthsEmployed()),
                BigDecimal.valueOf(props.getMinMonthsEmployed()), months);
    }
}

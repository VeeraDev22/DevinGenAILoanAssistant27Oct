package com.example.loanassistant.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.example.loanassistant.config.LendingRuleProperties;
import com.example.loanassistant.domain.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class DebtToIncomeRule implements LendingRule {

    private final LendingRuleProperties props;

    public DebtToIncomeRule(LendingRuleProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "debt-to-income";
    }

    @Override
    public RuleResult evaluate(LoanApplication app) {
        BigDecimal income = app.getAnnualIncome();
        if (income == null || income.signum() <= 0) {
            return RuleResult.fail(name(), "Annual income must be positive to compute DTI",
                    props.getMaxDtiRatio(), null);
        }
        BigDecimal dti = app.getMonthlyDebtPayments()
                .multiply(BigDecimal.valueOf(12))
                .divide(income, 4, RoundingMode.HALF_UP);
        if (dti.compareTo(props.getMaxDtiRatio()) > 0) {
            return RuleResult.fail(name(),
                    "DTI ratio %s exceeds the maximum of %s".formatted(dti, props.getMaxDtiRatio()),
                    props.getMaxDtiRatio(), dti);
        }
        if (dti.compareTo(props.getReviewDtiRatio()) > 0) {
            return RuleResult.review(name(),
                    "DTI ratio %s is in the manual-review band (%s-%s)"
                            .formatted(dti, props.getReviewDtiRatio(), props.getMaxDtiRatio()),
                    props.getReviewDtiRatio(), dti);
        }
        return RuleResult.pass(name(),
                "DTI ratio %s is within the limit of %s".formatted(dti, props.getReviewDtiRatio()),
                props.getReviewDtiRatio(), dti);
    }
}

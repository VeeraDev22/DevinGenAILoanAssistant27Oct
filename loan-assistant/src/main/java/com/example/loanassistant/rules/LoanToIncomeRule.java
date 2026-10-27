package com.example.loanassistant.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.example.loanassistant.config.LendingRuleProperties;
import com.example.loanassistant.domain.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class LoanToIncomeRule implements LendingRule {

    private final LendingRuleProperties props;

    public LoanToIncomeRule(LendingRuleProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "loan-to-income";
    }

    @Override
    public RuleResult evaluate(LoanApplication app) {
        BigDecimal income = app.getAnnualIncome();
        if (income == null || income.signum() <= 0) {
            return RuleResult.fail(name(), "Annual income must be positive to compute loan-to-income",
                    props.getMaxLoanToIncomeRatio(), null);
        }
        BigDecimal ratio = app.getLoanAmount().divide(income, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(props.getMaxLoanToIncomeRatio()) > 0) {
            return RuleResult.fail(name(),
                    "Loan-to-income ratio %s exceeds the maximum of %s"
                            .formatted(ratio, props.getMaxLoanToIncomeRatio()),
                    props.getMaxLoanToIncomeRatio(), ratio);
        }
        if (ratio.compareTo(props.getReviewLoanToIncomeRatio()) > 0) {
            return RuleResult.review(name(),
                    "Loan-to-income ratio %s is in the manual-review band (%s-%s)"
                            .formatted(ratio, props.getReviewLoanToIncomeRatio(), props.getMaxLoanToIncomeRatio()),
                    props.getReviewLoanToIncomeRatio(), ratio);
        }
        return RuleResult.pass(name(),
                "Loan-to-income ratio %s is within the limit of %s"
                        .formatted(ratio, props.getReviewLoanToIncomeRatio()),
                props.getReviewLoanToIncomeRatio(), ratio);
    }
}

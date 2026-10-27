package com.example.loanassistant.rules;

import java.math.BigDecimal;

import com.example.loanassistant.config.LendingRuleProperties;
import com.example.loanassistant.domain.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class CreditScoreRule implements LendingRule {

    private final LendingRuleProperties props;

    public CreditScoreRule(LendingRuleProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "credit-score";
    }

    @Override
    public RuleResult evaluate(LoanApplication app) {
        int score = app.getCreditScore();
        BigDecimal actual = BigDecimal.valueOf(score);
        if (score < props.getMinCreditScore()) {
            return RuleResult.fail(name(),
                    "Credit score %d is below the minimum of %d".formatted(score, props.getMinCreditScore()),
                    BigDecimal.valueOf(props.getMinCreditScore()), actual);
        }
        if (score < props.getReviewCreditScore()) {
            return RuleResult.review(name(),
                    "Credit score %d is in the manual-review band (%d-%d)"
                            .formatted(score, props.getMinCreditScore(), props.getReviewCreditScore()),
                    BigDecimal.valueOf(props.getReviewCreditScore()), actual);
        }
        return RuleResult.pass(name(),
                "Credit score %d meets the threshold of %d".formatted(score, props.getReviewCreditScore()),
                BigDecimal.valueOf(props.getReviewCreditScore()), actual);
    }
}

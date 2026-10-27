package com.example.loanassistant.config;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable lending-policy thresholds. Every evaluation persists the
 * threshold in effect at decision time for auditability.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "lending.rules")
public class LendingRuleProperties {

    private int minCreditScore = 640;
    private int reviewCreditScore = 700;
    private BigDecimal maxDtiRatio = new BigDecimal("0.43");
    private BigDecimal reviewDtiRatio = new BigDecimal("0.36");
    private BigDecimal minAnnualIncome = new BigDecimal("30000");
    private BigDecimal minLoanAmount = new BigDecimal("1000");
    private BigDecimal maxLoanAmount = new BigDecimal("250000");
    private BigDecimal maxLoanToIncomeRatio = new BigDecimal("4.0");
    private BigDecimal reviewLoanToIncomeRatio = new BigDecimal("3.0");
    private int minMonthsEmployed = 12;
    private int maxTermMonths = 84;
}

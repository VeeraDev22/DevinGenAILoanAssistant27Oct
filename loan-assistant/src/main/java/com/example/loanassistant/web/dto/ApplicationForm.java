package com.example.loanassistant.web.dto;

import java.math.BigDecimal;

import com.example.loanassistant.domain.EmploymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Loan application intake form. No protected attributes (race, religion,
 * gender, disability, marital status, etc.) are collected.
 */
@Data
public class ApplicationForm {

    @NotBlank
    @Size(max = 128)
    private String applicantName;

    @NotBlank
    @Email
    @Size(max = 128)
    private String applicantEmail;

    @NotNull
    @DecimalMin("0")
    private BigDecimal annualIncome;

    @NotNull
    @DecimalMin("0")
    private BigDecimal monthlyDebtPayments;

    @NotNull
    @Min(300)
    @Max(850)
    private Integer creditScore;

    @NotNull
    private EmploymentStatus employmentStatus;

    @Size(max = 128)
    private String employerName;

    @NotNull
    @Min(0)
    private Integer monthsEmployed;

    @NotNull
    @DecimalMin("1")
    private BigDecimal loanAmount;

    @NotNull
    @Min(1)
    @Max(360)
    private Integer termMonths;

    @NotBlank
    @Size(max = 64)
    private String loanPurpose;
}

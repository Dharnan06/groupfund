package com.nammakuzhu.groupModule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class LoanDetailsResponse {
    private String loanSource;
    private BigDecimal loanAmount;
    private Integer durationMonths;
    private BigDecimal monthlyEmi;
    private BigDecimal interestRate;
    private LocalDate loanStartDate;
}

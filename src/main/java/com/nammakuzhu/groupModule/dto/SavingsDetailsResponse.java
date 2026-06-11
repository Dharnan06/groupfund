package com.nammakuzhu.groupModule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SavingsDetailsResponse {
    private BigDecimal monthlySavingsAmount;
    private LocalDate savingsStartDate;
}

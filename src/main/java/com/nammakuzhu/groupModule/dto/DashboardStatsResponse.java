package com.nammakuzhu.groupModule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DashboardStatsResponse {
    private Integer totalGroups;
    private BigDecimal monthlySavings;
    private BigDecimal activeLoan;
    private BigDecimal pendingDues;
}

package com.nammakuzhu.groupModule.dto;

import com.nammakuzhu.groupModule.enums.GroupType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateGroupRequest {

    private String groupName;

    private GroupType groupType;

    private LocalDate startDate;
    private Integer targetMembers;

    // Savings group fields
    private BigDecimal monthlySavingsAmount;

    // Custom loan fields
    private String loanSource;
    private BigDecimal loanAmount;
    private Integer durationMonths;
    private BigDecimal monthlyEmi;
    private BigDecimal interestRate;
}
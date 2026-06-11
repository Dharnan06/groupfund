package com.nammakuzhu.groupModule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "custom_loan_details")
public class CustomLoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private GroupEntity group;

    private String loanSource;

    private BigDecimal loanAmount;

    private Integer durationMonths;

    private BigDecimal monthlyEmi;

    private BigDecimal interestRate;

    private LocalDate loanStartDate;
}
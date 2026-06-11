package com.nammakuzhu.paymentModule.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentRequest {
    private Long groupId;
    private Long userId;
    private BigDecimal amount;
    private LocalDate paymentMonth;
    private String notes;
}

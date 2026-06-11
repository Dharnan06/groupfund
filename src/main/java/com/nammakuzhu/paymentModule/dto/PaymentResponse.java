package com.nammakuzhu.paymentModule.dto;

import com.nammakuzhu.paymentModule.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private Long groupId;
    private String groupName;
    private Long userId;
    private String fullName;
    private BigDecimal amount;
    private LocalDate paymentMonth;
    private LocalDate paidDate;
    private PaymentStatus status;
    private String notes;
}

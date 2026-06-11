package com.nammakuzhu.paymentModule.dto;

import com.nammakuzhu.groupModule.enums.MemberRole;
import com.nammakuzhu.paymentModule.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentMemberSummaryResponse {
    private Long userId;
    private String fullName;
    private MemberRole role;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private PaymentStatus status;
}

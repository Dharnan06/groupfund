package com.nammakuzhu.paymentModule.entity;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.paymentModule.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "group_payments")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private GroupEntity group;

    @ManyToOne
    private AuthEntity user;

    private BigDecimal amount;

    private LocalDate paymentMonth;

    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String notes;

    private LocalDateTime createdAt;
}

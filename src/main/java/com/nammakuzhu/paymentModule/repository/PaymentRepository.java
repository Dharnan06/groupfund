package com.nammakuzhu.paymentModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.paymentModule.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByGroupOrderByPaymentMonthDescCreatedAtDesc(GroupEntity group);

    List<PaymentEntity> findByGroupAndPaymentMonthOrderByCreatedAtDesc(GroupEntity group, LocalDate paymentMonth);

    List<PaymentEntity> findByGroupAndUserAndPaymentMonth(GroupEntity group, AuthEntity user, LocalDate paymentMonth);

    List<PaymentEntity> findByGroupAndUserOrderByPaymentMonthDescCreatedAtDesc(GroupEntity group, AuthEntity user);

    void deleteByGroup(GroupEntity group);
}

package com.nammakuzhu.paymentModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.paymentModule.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    List<PaymentEntity> findByGroupOrderByPaymentMonthDescCreatedAtDesc(GroupEntity group);

    List<PaymentEntity> findByGroupAndPaymentMonthOrderByCreatedAtDesc(GroupEntity group, LocalDate paymentMonth);

    List<PaymentEntity> findByGroupAndUserAndPaymentMonth(GroupEntity group, AuthEntity user, LocalDate paymentMonth);

    List<PaymentEntity> findByGroupAndUserOrderByPaymentMonthDescCreatedAtDesc(GroupEntity group, AuthEntity user);

    void deleteByGroup(GroupEntity group);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM PaymentEntity p
        WHERE p.user = :user
        AND p.group = :group
        AND p.paymentMonth = :paymentMonth
    """)
    BigDecimal sumPaidAmountByGroupUserAndMonth(
            @Param("group") GroupEntity group,
            @Param("user") AuthEntity user,
            @Param("paymentMonth") LocalDate paymentMonth
    );
}
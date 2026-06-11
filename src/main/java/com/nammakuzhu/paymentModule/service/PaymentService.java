package com.nammakuzhu.paymentModule.service;

import com.nammakuzhu.authModule.dto.ApiResponse;
import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.authModule.repository.AuthRepository;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.groupModule.entity.GroupMemberEntity;
import com.nammakuzhu.groupModule.enums.GroupStatus;
import com.nammakuzhu.groupModule.enums.GroupType;
import com.nammakuzhu.groupModule.enums.MemberStatus;
import com.nammakuzhu.groupModule.repository.CustomLoanRepository;
import com.nammakuzhu.groupModule.repository.GroupMemberRepository;
import com.nammakuzhu.groupModule.repository.GroupRepository;
import com.nammakuzhu.groupModule.repository.SavingsGroupRepository;
import com.nammakuzhu.paymentModule.dto.PaymentMemberSummaryResponse;
import com.nammakuzhu.paymentModule.dto.PaymentRequest;
import com.nammakuzhu.paymentModule.dto.PaymentResponse;
import com.nammakuzhu.paymentModule.entity.PaymentEntity;
import com.nammakuzhu.paymentModule.enums.PaymentStatus;
import com.nammakuzhu.paymentModule.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SavingsGroupRepository savingsGroupRepository;
    private final CustomLoanRepository customLoanRepository;
    private final AuthRepository authRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            SavingsGroupRepository savingsGroupRepository,
            CustomLoanRepository customLoanRepository,
            AuthRepository authRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.savingsGroupRepository = savingsGroupRepository;
        this.customLoanRepository = customLoanRepository;
        this.authRepository = authRepository;
    }

    private AuthEntity getLoggedInUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return authRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    public ResponseEntity<?> listGroupPayments(Long groupId, LocalDate paymentMonth) {
        AuthEntity user = getLoggedInUser();
        GroupEntity group = getGroup(groupId);

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(false, "You are not allowed to view payments for this group", null));
        }

        List<PaymentEntity> payments = paymentMonth == null
                ? paymentRepository.findByGroupOrderByPaymentMonthDescCreatedAtDesc(group)
                : paymentRepository.findByGroupAndPaymentMonthOrderByCreatedAtDesc(group, normalizeMonth(paymentMonth));

        List<PaymentResponse> response = payments.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new ApiResponse(true, "Payments fetched successfully", response));
    }

    public ResponseEntity<?> listGroupPaymentSummary(Long groupId, LocalDate paymentMonth) {
        AuthEntity user = getLoggedInUser();
        GroupEntity group = getGroup(groupId);

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(false, "You are not allowed to view payment summary for this group", null));
        }

        LocalDate selectedMonth = paymentMonth == null
                ? YearMonth.now().atDay(1)
                : normalizeMonth(paymentMonth);

        BigDecimal expectedAmount = expectedMonthlyAmount(group);

        List<PaymentMemberSummaryResponse> response = groupMemberRepository.findByGroup(group).stream()
                .filter(member -> member.getStatus() == MemberStatus.ACCEPTED)
                .map(member -> toSummaryResponse(group, member, expectedAmount, selectedMonth))
                .toList();

        return ResponseEntity.ok(new ApiResponse(true, "Payment summary fetched successfully", response));
    }

    public ResponseEntity<?> listMyGroupPayments(Long groupId) {
        AuthEntity user = getLoggedInUser();
        GroupEntity group = getGroup(groupId);

        boolean loggedInUserIsGroupMember = groupMemberRepository.findByGroup(group).stream()
                .anyMatch(member -> member.getUser().getId().equals(user.getId())
                        && member.getStatus() == MemberStatus.ACCEPTED);

        if (!loggedInUserIsGroupMember) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(false, "You are not allowed to view payment history for this group", null));
        }

        List<PaymentResponse> response = paymentRepository
                .findByGroupAndUserOrderByPaymentMonthDescCreatedAtDesc(group, user)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new ApiResponse(true, "My payment history fetched successfully", response));
    }

    public ResponseEntity<?> recordPayment(PaymentRequest request) {
        AuthEntity loggedInUser = getLoggedInUser();

        if (request.getGroupId() == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Group is required", null));
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Payment amount must be greater than zero", null));
        }

        GroupEntity group = getGroup(request.getGroupId());

        if (group.getGroupStatus() != GroupStatus.ACTIVE) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Payments can be recorded only after the group is active", null));
        }

        boolean loggedInUserIsGroupMember = groupMemberRepository.findByGroup(group).stream()
                .anyMatch(member -> member.getUser().getId().equals(loggedInUser.getId())
                        && member.getStatus() == MemberStatus.ACCEPTED);

        if (!loggedInUserIsGroupMember) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(false, "You are not an accepted member of this group", null));
        }

        boolean loggedInUserIsLeader = group.getLeader().getId().equals(loggedInUser.getId());

        AuthEntity paymentUser = request.getUserId() == null
                ? loggedInUser
                : authRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Payment user not found"));

        if (!loggedInUserIsLeader) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(false, "Only the group leader can record payments", null));
        }

        boolean targetIsMember = groupMemberRepository.findByGroup(group).stream()
                .anyMatch(member -> member.getUser().getId().equals(paymentUser.getId())
                        && member.getStatus() == MemberStatus.ACCEPTED);

        if (!targetIsMember) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Selected user is not an accepted group member", null));
        }

        LocalDate paymentMonth = request.getPaymentMonth() == null
                ? YearMonth.now().atDay(1)
                : normalizeMonth(request.getPaymentMonth());

        BigDecimal expectedAmount = expectedMonthlyAmount(group);
        BigDecimal alreadyPaid = getPaidAmount(group, paymentUser, paymentMonth);

        if (expectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "This group does not have a valid monthly amount configured", null));
        }

        if (alreadyPaid.compareTo(BigDecimal.ZERO) > 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Payment is already recorded for this member and month", null));
        }

        if (request.getAmount().compareTo(expectedAmount) != 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Amount must be exactly ₹" + expectedAmount + " for this month", null));
        }

        PaymentStatus status = PaymentStatus.PAID;

        PaymentEntity payment = new PaymentEntity();
        payment.setGroup(group);
        payment.setUser(paymentUser);
        payment.setAmount(request.getAmount());
        payment.setPaymentMonth(paymentMonth);
        payment.setPaidDate(LocalDate.now());
        payment.setStatus(status);
        payment.setNotes(request.getNotes());
        payment.setCreatedAt(java.time.LocalDateTime.now());

        paymentRepository.save(payment);

        return ResponseEntity.ok(new ApiResponse(true, "Payment recorded successfully", toResponse(payment)));
    }

    public BigDecimal getPendingDues(AuthEntity user, List<GroupEntity> groups) {
        LocalDate currentMonth = YearMonth.now().atDay(1);

        return groups.stream()
                .map(group -> pendingForGroupAndUser(group, user, currentMonth))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private GroupEntity getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    private PaymentMemberSummaryResponse toSummaryResponse(
            GroupEntity group,
            GroupMemberEntity member,
            BigDecimal expectedAmount,
            LocalDate selectedMonth
    ) {
        BigDecimal paidAmount = getPaidAmount(group, member.getUser(), selectedMonth);
        BigDecimal pendingAmount = group.getGroupStatus() == GroupStatus.ACTIVE ? expectedAmount.subtract(paidAmount) : BigDecimal.ZERO;

        if (pendingAmount.compareTo(BigDecimal.ZERO) < 0) {
            pendingAmount = BigDecimal.ZERO;
        }

        return new PaymentMemberSummaryResponse(
                member.getUser().getId(),
                member.getUser().getName(),
                member.getRole(),
                expectedAmount,
                paidAmount,
                pendingAmount,
                calculateStatus(expectedAmount, paidAmount)
        );
    }

    private BigDecimal pendingForGroupAndUser(GroupEntity group, AuthEntity user, LocalDate paymentMonth) {
        if (group.getGroupStatus() != GroupStatus.ACTIVE) {
            return BigDecimal.ZERO;
        }
        BigDecimal expectedAmount = expectedMonthlyAmount(group);

        if (expectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal pendingAmount = expectedAmount.subtract(getPaidAmount(group, user, paymentMonth));
        return pendingAmount.compareTo(BigDecimal.ZERO) > 0 ? pendingAmount : BigDecimal.ZERO;
    }

    private BigDecimal getPaidAmount(GroupEntity group, AuthEntity user, LocalDate paymentMonth) {
        return nullToZero(
                paymentRepository.sumPaidAmountByGroupUserAndMonth(group, user, paymentMonth)
        );
    }

    private PaymentStatus calculateStatus(BigDecimal expectedAmount, BigDecimal paidAmount) {
        if (expectedAmount == null || expectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return paidAmount.compareTo(BigDecimal.ZERO) > 0 ? PaymentStatus.PAID : PaymentStatus.PENDING;
        }

        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentStatus.PENDING;
        }

        if (paidAmount.compareTo(expectedAmount) >= 0) {
            return PaymentStatus.PAID;
        }

        return PaymentStatus.PARTIAL;
    }

    private BigDecimal expectedMonthlyAmount(GroupEntity group) {
        if (group.getGroupType() == GroupType.SAVINGS) {
            return savingsGroupRepository.findByGroup(group)
                    .map(savings -> nullToZero(savings.getMonthlySavingsAmount()))
                    .orElse(BigDecimal.ZERO);
        }

        if (group.getGroupType() == GroupType.CUSTOM_LOAN) {
            return customLoanRepository.findByGroup(group)
                    .map(loan -> nullToZero(loan.getMonthlyEmi()))
                    .orElse(BigDecimal.ZERO);
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal nullToZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private LocalDate normalizeMonth(LocalDate date) {
        return YearMonth.from(date).atDay(1);
    }

    private PaymentResponse toResponse(PaymentEntity payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getGroup().getId(),
                payment.getGroup().getGroupName(),
                payment.getUser().getId(),
                payment.getUser().getName(),
                payment.getAmount(),
                payment.getPaymentMonth(),
                payment.getPaidDate(),
                payment.getStatus(),
                payment.getNotes()
        );
    }
}

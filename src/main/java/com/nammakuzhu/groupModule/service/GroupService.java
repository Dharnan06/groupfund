package com.nammakuzhu.groupModule.service;

import org.springframework.transaction.annotation.Transactional;
import com.nammakuzhu.authModule.dto.ApiResponse;
import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.authModule.repository.AuthRepository;
import com.nammakuzhu.groupModule.dto.*;
import com.nammakuzhu.groupModule.entity.CustomLoanEntity;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.groupModule.entity.GroupMemberEntity;
import com.nammakuzhu.groupModule.entity.SavingsGroupEntity;
import com.nammakuzhu.groupModule.enums.GroupStatus;
import com.nammakuzhu.groupModule.enums.GroupType;
import com.nammakuzhu.groupModule.enums.MemberRole;
import com.nammakuzhu.groupModule.enums.MemberStatus;
import com.nammakuzhu.groupModule.repository.CustomLoanRepository;
import com.nammakuzhu.groupModule.repository.GroupMemberRepository;
import com.nammakuzhu.groupModule.repository.GroupRepository;
import com.nammakuzhu.groupModule.repository.SavingsGroupRepository;
import com.nammakuzhu.paymentModule.repository.PaymentRepository;
import com.nammakuzhu.paymentModule.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GroupService {

    private static final int MIN_GROUP_MEMBERS = 5;
    private static final int MAX_GROUP_MEMBERS = 20;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SavingsGroupRepository savingsGroupRepository;
    private final CustomLoanRepository customLoanRepository;
    private final AuthRepository authRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            SavingsGroupRepository savingsGroupRepository,
            CustomLoanRepository customLoanRepository,
            AuthRepository authRepository,
            PaymentRepository paymentRepository,
            PaymentService paymentService
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.savingsGroupRepository = savingsGroupRepository;
        this.customLoanRepository = customLoanRepository;
        this.authRepository = authRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    private AuthEntity getLoggedInUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return authRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    public ResponseEntity<?> createGroup(CreateGroupRequest request) {
        AuthEntity leader = getLoggedInUser();

        if (request.getGroupName() == null || request.getGroupName().isBlank()) {
            return bad("Group name is required");
        }
        if (request.getGroupType() == null) {
            return bad("Group type is required");
        }
        Integer targetMembers = request.getTargetMembers();
        if (targetMembers == null) targetMembers = MIN_GROUP_MEMBERS;
        if (targetMembers < MIN_GROUP_MEMBERS || targetMembers > MAX_GROUP_MEMBERS) {
            return bad("Group member limit must be between 5 and 20");
        }
        if (request.getStartDate() == null) {
            request.setStartDate(java.time.LocalDate.now());
        }

        if (request.getGroupType() == GroupType.SAVINGS) {
            if (request.getMonthlySavingsAmount() == null || request.getMonthlySavingsAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return bad("Monthly savings amount must be greater than zero");
            }
        }

        if (request.getGroupType() == GroupType.CUSTOM_LOAN) {
            if (request.getLoanSource() == null || request.getLoanSource().isBlank()) return bad("Loan source is required");
            if (request.getLoanAmount() == null || request.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0) return bad("Loan amount must be greater than zero");
            if (request.getDurationMonths() == null || request.getDurationMonths() <= 0) return bad("Loan duration must be greater than zero");
            if (request.getMonthlyEmi() == null || request.getMonthlyEmi().compareTo(BigDecimal.ZERO) <= 0) {
                request.setMonthlyEmi(calculateSimpleMonthlyEmi(request.getLoanAmount(), request.getInterestRate(), request.getDurationMonths()));
            }
        }

        GroupEntity group = new GroupEntity();
        group.setGroupName(request.getGroupName());
        group.setGroupType(request.getGroupType());
        group.setGroupStatus(GroupStatus.PENDING_MEMBERS);
        group.setLeader(leader);
        group.setTotalMembers(1);
        group.setTargetMembers(targetMembers);
        group.setStartDate(request.getStartDate());
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);

        GroupMemberEntity leaderMember = new GroupMemberEntity();
        leaderMember.setGroup(group);
        leaderMember.setUser(leader);
        leaderMember.setRole(MemberRole.LEADER);
        leaderMember.setStatus(MemberStatus.ACCEPTED);
        leaderMember.setJoinedAt(LocalDateTime.now());
        leaderMember.setActivationApproved(false);
        groupMemberRepository.save(leaderMember);

        if (request.getGroupType() == GroupType.SAVINGS) {
            SavingsGroupEntity savings = new SavingsGroupEntity();
            savings.setGroup(group);
            savings.setMonthlySavingsAmount(request.getMonthlySavingsAmount());
            savings.setSavingsStartDate(request.getStartDate());
            savingsGroupRepository.save(savings);
        }

        if (request.getGroupType() == GroupType.CUSTOM_LOAN) {
            CustomLoanEntity loan = new CustomLoanEntity();
            loan.setGroup(group);
            loan.setLoanSource(request.getLoanSource());
            loan.setLoanAmount(request.getLoanAmount());
            loan.setDurationMonths(request.getDurationMonths());
            loan.setMonthlyEmi(request.getMonthlyEmi());
            loan.setInterestRate(request.getInterestRate());
            loan.setLoanStartDate(request.getStartDate());
            customLoanRepository.save(loan);
        }

        return ResponseEntity.ok(new ApiResponse(true, "Group created successfully. Invite at least 4 more members, then request activation.", toResponse(group, leader)));
    }

    private GroupResponse toResponse(GroupEntity group, AuthEntity loggedInUser) {
        long acceptedCount = groupMemberRepository.countByGroupAndStatus(group, MemberStatus.ACCEPTED);

        syncGroupReadiness(group, acceptedCount);

        return new GroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getGroupType(),
                group.getGroupStatus(),
                group.getLeader().getName(),
                (int) acceptedCount,
                safeTargetMembers(group),
                group.getStartDate(),
                group.getLeader().getId().equals(loggedInUser.getId())
        );
    }

    public ResponseEntity<?> inviteMember(InviteMemberRequest request) {
        AuthEntity leader = getLoggedInUser();

        GroupEntity group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getLeader().getId().equals(leader.getId())) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(false, "Only group leader can invite members", null));
        }

        if (group.getGroupStatus() == GroupStatus.ACTIVATION_PENDING) {
            return bad("Activation voting is in progress. Finish activation before inviting more members.");
        }

        AuthEntity invitedUser = authRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (leader.getId().equals(invitedUser.getId())) {
            return bad("Leader is already part of the group");
        }

        if (groupMemberRepository.existsByGroupAndUser(group, invitedUser)) {
            return ResponseEntity.status(409)
                    .body(new ApiResponse(false, "User already invited or already in group", null));
        }

        long activeOrPendingMembers = groupMemberRepository.countByGroupAndStatusIn(
                group,
                List.of(MemberStatus.ACCEPTED, MemberStatus.PENDING)
        );

        if (activeOrPendingMembers >= safeTargetMembers(group)) {
            return bad("This group already reached the selected member limit of " + safeTargetMembers(group));
        }

        GroupMemberEntity member = new GroupMemberEntity();
        member.setGroup(group);
        member.setUser(invitedUser);
        member.setRole(MemberRole.MEMBER);
        member.setStatus(MemberStatus.PENDING);
        member.setInvitedAt(LocalDateTime.now());
        member.setActivationApproved(false);

        groupMemberRepository.save(member);

        return ResponseEntity.ok(
                new ApiResponse(true, "Group invitation sent successfully", null)
        );
    }

    public ResponseEntity<?> getMyPendingInvitations() {
        AuthEntity user = getLoggedInUser();

        List<GroupInvitationResponse> response =
                groupMemberRepository.findByUserAndStatusWithGroupAndLeader(user, MemberStatus.PENDING)
                        .stream()
                        .map(invitation -> new GroupInvitationResponse(
                                invitation.getId(),
                                invitation.getGroup().getId(),
                                invitation.getGroup().getGroupName(),
                                invitation.getGroup().getGroupType(),
                                invitation.getGroup().getLeader().getName()
                        ))
                        .toList();

        return ResponseEntity.ok(
                new ApiResponse(true, "Group invitations fetched successfully", response)
        );
    }

    public ResponseEntity<?> acceptGroupInvitation(Long invitationId) {
        AuthEntity user = getLoggedInUser();
        GroupMemberEntity invitation = groupMemberRepository.findById(invitationId).orElseThrow(() -> new RuntimeException("Invitation not found"));
        if (!invitation.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).body(new ApiResponse(false, "You are not allowed to accept this invitation", null));
        if (invitation.getStatus() != MemberStatus.PENDING) return bad("Invitation is already processed");

        GroupEntity group = invitation.getGroup();
        long acceptedBefore = groupMemberRepository.countByGroupAndStatus(group, MemberStatus.ACCEPTED);
        if (acceptedBefore >= safeTargetMembers(group)) return bad("This group already reached the selected member limit of " + safeTargetMembers(group));

        invitation.setStatus(MemberStatus.ACCEPTED);
        invitation.setJoinedAt(LocalDateTime.now());
        invitation.setActivationApproved(false);
        groupMemberRepository.save(invitation);

        long acceptedCount = groupMemberRepository.countByGroupAndStatus(group, MemberStatus.ACCEPTED);
        syncGroupReadiness(group, acceptedCount);
        return ResponseEntity.ok(new ApiResponse(true, "Group invitation accepted successfully", null));
    }

    public ResponseEntity<?> rejectGroupInvitation(Long invitationId) {
        AuthEntity user = getLoggedInUser();
        GroupMemberEntity invitation = groupMemberRepository.findById(invitationId).orElseThrow(() -> new RuntimeException("Invitation not found"));
        if (!invitation.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).body(new ApiResponse(false, "You are not allowed to reject this invitation", null));
        if (invitation.getStatus() != MemberStatus.PENDING) return bad("Invitation is already processed");
        invitation.setStatus(MemberStatus.REJECTED);
        groupMemberRepository.save(invitation);
        return ResponseEntity.ok(new ApiResponse(true, "Group invitation rejected successfully", null));
    }

    public ResponseEntity<?> getMyGroups() {
        AuthEntity user = getLoggedInUser();

        List<GroupResponse> response =
                groupMemberRepository.findByUserAndStatusWithGroupAndLeader(user, MemberStatus.ACCEPTED)
                        .stream()
                        .map(member -> toResponse(member.getGroup(), user))
                        .toList();

        return ResponseEntity.ok(
                new ApiResponse(true, "My groups fetched successfully", response)
        );
    }

    public ResponseEntity<?> getDashboardStats() {
        AuthEntity user = getLoggedInUser();

        List<GroupEntity> groups =
                groupMemberRepository.findAcceptedGroupsForDashboard(user, MemberStatus.ACCEPTED)
                        .stream()
                        .map(GroupMemberEntity::getGroup)
                        .toList();

        if (groups.isEmpty()) {
            DashboardStatsResponse response = new DashboardStatsResponse(
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );

            return ResponseEntity.ok(
                    new ApiResponse(true, "Dashboard stats fetched successfully", response)
            );
        }

        BigDecimal monthlySavings = savingsGroupRepository.sumMonthlySavingsByGroups(groups);
        BigDecimal activeLoan = customLoanRepository.sumLoanAmountByGroups(groups);
        BigDecimal pendingDues = paymentService.getPendingDues(user, groups);

        DashboardStatsResponse response = new DashboardStatsResponse(
                groups.size(),
                nullToZero(monthlySavings),
                nullToZero(activeLoan),
                nullToZero(pendingDues)
        );

        return ResponseEntity.ok(
                new ApiResponse(true, "Dashboard stats fetched successfully", response)
        );
    }
    public ResponseEntity<?> getGroupDetails(Long groupId) {
        AuthEntity user = getLoggedInUser();

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!groupMemberRepository.existsByGroupAndUser(group, user)) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(false, "You are not allowed to view this group", null));
        }

        List<GroupMemberEntity> groupMembers = groupMemberRepository.findByGroupWithUser(group);

        List<GroupMemberResponse> members = groupMembers.stream()
                .map(member -> new GroupMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getName(),
                        member.getUser().getMobileNumber(),
                        member.getRole(),
                        member.getStatus()
                ))
                .toList();

        long acceptedCount = groupMembers.stream()
                .filter(member -> member.getStatus() == MemberStatus.ACCEPTED)
                .count();

        syncGroupReadiness(group, acceptedCount);

        SavingsDetailsResponse savingsDetails = savingsGroupRepository.findByGroup(group)
                .map(s -> new SavingsDetailsResponse(
                        s.getMonthlySavingsAmount(),
                        s.getSavingsStartDate()
                ))
                .orElse(null);

        LoanDetailsResponse loanDetails = customLoanRepository.findByGroup(group)
                .map(l -> new LoanDetailsResponse(
                        l.getLoanSource(),
                        l.getLoanAmount(),
                        l.getDurationMonths(),
                        l.getMonthlyEmi(),
                        l.getInterestRate(),
                        l.getLoanStartDate()
                ))
                .orElse(null);

        MemberRole currentUserRole = groupMembers.stream()
                .filter(member -> member.getUser().getId().equals(user.getId())
                        && member.getStatus() == MemberStatus.ACCEPTED)
                .map(GroupMemberEntity::getRole)
                .findFirst()
                .orElse(null);

        boolean isLeader = group.getLeader().getId().equals(user.getId())
                || currentUserRole == MemberRole.LEADER;

        int activationRequired = (int) acceptedCount;

        int activationApproved = (int) groupMembers.stream()
                .filter(m -> m.getStatus() == MemberStatus.ACCEPTED
                        && Boolean.TRUE.equals(m.getActivationApproved()))
                .count();

        boolean currentApproved = groupMembers.stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId())
                        && Boolean.TRUE.equals(m.getActivationApproved()));

        GroupDetailsResponse response = new GroupDetailsResponse(
                group.getId(),
                group.getGroupName(),
                group.getGroupType(),
                group.getGroupStatus(),
                group.getLeader().getName(),
                (int) acceptedCount,
                safeTargetMembers(group),
                group.getStartDate(),
                isLeader,
                group.getLeader().getId(),
                user.getId(),
                currentUserRole,
                activationApproved,
                activationRequired,
                currentApproved,
                members,
                savingsDetails,
                loanDetails
        );

        return ResponseEntity.ok(
                new ApiResponse(true, "Group details fetched successfully", response)
        );
    }

    public ResponseEntity<?> requestActivation(Long groupId) {
        AuthEntity user = getLoggedInUser();
        GroupEntity group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        if (!group.getLeader().getId().equals(user.getId())) return ResponseEntity.status(403).body(new ApiResponse(false, "Only the group leader can request activation", null));
        long acceptedCount = groupMemberRepository.countByGroupAndStatus(group, MemberStatus.ACCEPTED);
        if (acceptedCount < MIN_GROUP_MEMBERS) return bad("At least 5 accepted members are required before activation request");
        if (group.getGroupStatus() == GroupStatus.ACTIVE) return bad("Group is already active");

        List<GroupMemberEntity> members = groupMemberRepository.findByGroup(group);
        members.stream().filter(m -> m.getStatus() == MemberStatus.ACCEPTED).forEach(m -> m.setActivationApproved(m.getUser().getId().equals(user.getId())));
        groupMemberRepository.saveAll(members);
        group.setGroupStatus(GroupStatus.ACTIVATION_PENDING);
        group.setActivationRequestedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        groupRepository.save(group);
        return ResponseEntity.ok(new ApiResponse(true, "Activation request sent. All accepted members must approve it.", null));
    }

    public ResponseEntity<?> approveActivation(Long groupId) {
        AuthEntity user = getLoggedInUser();
        GroupEntity group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        if (group.getGroupStatus() != GroupStatus.ACTIVATION_PENDING) return bad("Activation request is not currently open");
        GroupMemberEntity member = groupMemberRepository.findByGroup(group).stream()
                .filter(m -> m.getUser().getId().equals(user.getId()) && m.getStatus() == MemberStatus.ACCEPTED)
                .findFirst().orElse(null);
        if (member == null) return ResponseEntity.status(403).body(new ApiResponse(false, "Only accepted group members can approve activation", null));
        member.setActivationApproved(true);
        groupMemberRepository.save(member);

        List<GroupMemberEntity> acceptedMembers = groupMemberRepository.findByGroup(group).stream().filter(m -> m.getStatus() == MemberStatus.ACCEPTED).toList();
        boolean allApproved = !acceptedMembers.isEmpty() && acceptedMembers.stream().allMatch(m -> Boolean.TRUE.equals(m.getActivationApproved()));
        if (allApproved) {
            group.setGroupStatus(GroupStatus.ACTIVE);
            group.setUpdatedAt(LocalDateTime.now());
            groupRepository.save(group);
            return ResponseEntity.ok(new ApiResponse(true, "All members approved. Group is now active.", null));
        }
        return ResponseEntity.ok(new ApiResponse(true, "Activation approved successfully", null));
    }

    @Transactional
    public ResponseEntity<?> deleteGroup(Long groupId) {
        AuthEntity user = getLoggedInUser();
        GroupEntity group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        if (!group.getLeader().getId().equals(user.getId())) return ResponseEntity.status(403).body(new ApiResponse(false, "Only group leader can delete this group", null));
        if (group.getGroupStatus() == GroupStatus.ACTIVE) return bad("Active group cannot be deleted");
        savingsGroupRepository.deleteByGroup(group);
        customLoanRepository.deleteByGroup(group);
        paymentRepository.deleteByGroup(group);
        groupMemberRepository.deleteAll(groupMemberRepository.findByGroup(group));
        groupRepository.delete(group);
        return ResponseEntity.ok(new ApiResponse(true, "Group deleted successfully", null));
    }

    private void syncGroupReadiness(GroupEntity group, long acceptedCount) {
        boolean changed = false;
        if (group.getTargetMembers() == null || group.getTargetMembers() < MIN_GROUP_MEMBERS || group.getTargetMembers() > MAX_GROUP_MEMBERS) {
            group.setTargetMembers(MAX_GROUP_MEMBERS);
            changed = true;
        }
        if (group.getTotalMembers() == null || group.getTotalMembers() != (int) acceptedCount) {
            group.setTotalMembers((int) acceptedCount);
            changed = true;
        }
        if ((group.getGroupStatus() == GroupStatus.PENDING_MEMBERS || group.getGroupStatus() == null) && acceptedCount >= MIN_GROUP_MEMBERS) {
            group.setGroupStatus(GroupStatus.READY_TO_ACTIVATE);
            changed = true;
        }
        if (changed) {
            group.setUpdatedAt(LocalDateTime.now());
            groupRepository.save(group);
        }
    }

    private int safeTargetMembers(GroupEntity group) {
        Integer target = group.getTargetMembers();
        if (target == null || target < MIN_GROUP_MEMBERS || target > MAX_GROUP_MEMBERS) return MAX_GROUP_MEMBERS;
        return target;
    }

    private ResponseEntity<?> bad(String message) {
        return ResponseEntity.badRequest().body(new ApiResponse(false, message, null));
    }

    private BigDecimal calculateSimpleMonthlyEmi(BigDecimal loanAmount, BigDecimal interestRate, Integer durationMonths) {
        BigDecimal safeInterestRate = interestRate == null ? BigDecimal.ZERO : interestRate;
        BigDecimal totalInterest = loanAmount.multiply(safeInterestRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return loanAmount.add(totalInterest).divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}

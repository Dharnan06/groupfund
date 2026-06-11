package com.nammakuzhu.groupModule.dto;

import com.nammakuzhu.groupModule.enums.GroupStatus;
import com.nammakuzhu.groupModule.enums.GroupType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import com.nammakuzhu.groupModule.enums.MemberRole;

@Data
@AllArgsConstructor
public class GroupDetailsResponse {
    private Long groupId;
    private String groupName;
    private GroupType groupType;
    private GroupStatus groupStatus;
    private String leaderName;
    private Integer totalMembers;
    private Integer targetMembers;
    private LocalDate startDate;
    private Boolean leader;
    private Long leaderUserId;
    private Long currentUserId;
    private MemberRole currentUserRole;
    private Integer activationApprovedCount;
    private Integer activationRequiredCount;
    private Boolean currentUserActivationApproved;
    private List<GroupMemberResponse> members;
    private SavingsDetailsResponse savingsDetails;
    private LoanDetailsResponse loanDetails;
}

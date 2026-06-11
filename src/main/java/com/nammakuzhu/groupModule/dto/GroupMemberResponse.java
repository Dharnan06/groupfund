package com.nammakuzhu.groupModule.dto;

import com.nammakuzhu.groupModule.enums.MemberRole;
import com.nammakuzhu.groupModule.enums.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupMemberResponse {
    private Long userId;
    private String fullName;
    private String mobileNumber;
    private MemberRole role;
    private MemberStatus status;
}

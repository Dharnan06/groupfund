package com.nammakuzhu.groupModule.dto;

import com.nammakuzhu.groupModule.enums.GroupType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupInvitationResponse {

    private Long invitationId;
    private Long groupId;
    private String groupName;
    private GroupType groupType;
    private String leaderName;
}
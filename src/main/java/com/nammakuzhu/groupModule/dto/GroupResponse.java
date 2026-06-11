package com.nammakuzhu.groupModule.dto;

import com.nammakuzhu.groupModule.enums.GroupStatus;
import com.nammakuzhu.groupModule.enums.GroupType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class GroupResponse {

    private Long groupId;
    private String groupName;
    private GroupType groupType;
    private GroupStatus groupStatus;
    private String leaderName;
    private Integer totalMembers;
    private Integer targetMembers;
    private LocalDate startDate;
    private Boolean leader;

}
package com.nammakuzhu.groupModule.dto;

import lombok.Data;

@Data
public class InviteMemberRequest {

    private Long groupId;
    private Long userId;
}
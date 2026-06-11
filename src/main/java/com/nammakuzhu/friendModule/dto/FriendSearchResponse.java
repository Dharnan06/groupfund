package com.nammakuzhu.friendModule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FriendSearchResponse {

    private Long userId;
    private String fullName;
    private String mobileNumber;
    private String district;
    private String profileImageUrl;
    private String requestStatus;
}
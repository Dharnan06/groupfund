package com.nammakuzhu.profileModule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class ProfileResponse {

    private Long profileId;
    private String fullName;
    private String email;
    private String mobileNumber;

    private String profileImageUrl;

    private LocalDate dateOfBirth;
    private String gender;

    private String address;
    private String district;
    private String state;
    private String pincode;

    private String fatherName;
    private String motherName;
    private String occupation;
    private Double monthlyIncome;
    private String maritalStatus;
    private String aadhaarNumber;

    private boolean aadhaarNumberVerified;
    private boolean profileCompleted;
    private Integer profileCompletionPercentage;
}
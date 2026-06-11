package com.nammakuzhu.profileModule.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileRequest {

    private String fullName;

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
}
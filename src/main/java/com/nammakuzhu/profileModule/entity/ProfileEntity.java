package com.nammakuzhu.profileModule.entity;

import com.nammakuzhu.authModule.entity.AuthEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "user_profiles")
public class ProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AuthEntity user;

    private String fullName;

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

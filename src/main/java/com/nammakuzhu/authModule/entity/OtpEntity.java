package com.nammakuzhu.authModule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "otp_info")
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Integer otp;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private LocalDateTime expiryTime;

}

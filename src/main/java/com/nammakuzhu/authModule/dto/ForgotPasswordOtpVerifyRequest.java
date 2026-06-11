package com.nammakuzhu.authModule.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordOtpVerifyRequest {
    private String email;
    private Integer otp;
}

package com.nammakuzhu.authModule.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String name;
    private String mobileNumber;
    private String email;
    private String password;
    private String confirmPassword;
}

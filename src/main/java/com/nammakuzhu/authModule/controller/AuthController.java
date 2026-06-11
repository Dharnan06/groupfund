package com.nammakuzhu.authModule.controller;

import com.nammakuzhu.authModule.dto.*;
import com.nammakuzhu.authModule.service.AuthService;
import com.nammakuzhu.authModule.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    AuthController(AuthService authService,OtpService otpService){
        this.authService=authService;
        this.otpService=otpService;
    }

    @PostMapping("/register")
    public ResponseEntity<?>  register(@RequestBody RegisterRequest user){
        return authService.register(user);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest otpVerifyRequest){
        return otpService.verifyOtp(otpVerifyRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest){
        return authService.login(loginRequest);
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotPasswordOtp(@RequestBody ForgotPasswordRequest forgotPasswordRequest){
        return authService.sendForgotPasswordOtp(forgotPasswordRequest);
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(@RequestBody ForgotPasswordOtpVerifyRequest request) {
        return authService.verifyForgotPasswordOtp(request);
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @GetMapping("/test")
    public String test() {
        return "Protected API Working";
    }

}

package com.nammakuzhu.authModule.service;

import com.nammakuzhu.authModule.dto.*;
import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.authModule.repository.AuthRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final MailService mailService;
    private final JwtService jwtService;

    AuthService(AuthRepository authRepository,PasswordEncoder passwordEncoder,
                OtpService otpService,MailService mailService,
                JwtService jwtService){
        this.authRepository=authRepository;
        this.passwordEncoder=passwordEncoder;
        this.otpService=otpService;
        this.mailService=mailService;
        this.jwtService=jwtService;
    }

    public ResponseEntity<?> register(RegisterRequest request){
        //check password and confirm password
        if(!request.getPassword().equals( request.getConfirmPassword())){
            return ResponseEntity.status(422)
                    .body(new ApiResponse(false, "Password and confirm password do not match", null));
        }
        if(authRepository.existsByEmail(request.getEmail())){
            Optional<AuthEntity> presentUserInfo = authRepository.findByEmail(request.getEmail());
            if((presentUserInfo.get().isEmailVerified())){
                return ResponseEntity.status(409)
                        .body(new ApiResponse(false, "An account already exists with this email address", null));
            }
            else{
                presentUserInfo.get().setName(request.getName());
                presentUserInfo.get().setPassword(passwordEncoder.encode(request.getPassword()));
                presentUserInfo.get().setEmail(request.getEmail());
                presentUserInfo.get().setMobileNumber(request.getMobileNumber());
                presentUserInfo.get().setEmailVerified(false);
                authRepository.save(presentUserInfo.get());
            }
        }
        else if(authRepository.existsByMobileNumber(request.getMobileNumber())){
            Optional<AuthEntity> presentUserInfo = authRepository.findByMobileNumber(request.getMobileNumber());
            if((presentUserInfo.get().isEmailVerified())){
                return ResponseEntity.status(409)
                        .body(new ApiResponse(false, "An account already exists with this mobile number", null));
            }
            else{
                presentUserInfo.get().setName(request.getName());
                presentUserInfo.get().setPassword(passwordEncoder.encode(request.getPassword()));
                presentUserInfo.get().setEmail(request.getEmail());
                presentUserInfo.get().setMobileNumber(request.getMobileNumber());
                presentUserInfo.get().setEmailVerified(false);
                authRepository.save(presentUserInfo.get());
            }
        }
        else{
            AuthEntity user = new AuthEntity();
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setName(request.getName());
            user.setMobileNumber(request.getMobileNumber());
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
            authRepository.save(user);
        }

        Integer otp = otpService.generateAndSaveOtp(request.getEmail());
        mailService.sendProfessionalOtpEmail(request.getEmail(), otp);
        return ResponseEntity.status(201)
                .body(new ApiResponse(true, "Registration successful. OTP sent to your email.", null));
    }

    public ResponseEntity<?> login(LoginRequest loginRequest){
        Optional<AuthEntity> user = authRepository.findByEmail(loginRequest.getEmail());
        if(user.isEmpty()){
            return ResponseEntity.status(404)
                    .body(new ApiResponse(false, "No account found with this email address", null));
        }
        if(!user.get().isEmailVerified()){
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "Please verify your email before login", null));
        }
        if(!passwordEncoder.matches(loginRequest.getPassword(),user.get().getPassword())){
            return ResponseEntity.status(422).body(new ApiResponse(false, "You entered the wrong password", null));
        }

        String token = jwtService.generateToken(loginRequest.getEmail());

        LoginResponse loginResponse = new LoginResponse(token, user.get().getEmail(), user.get().getId(), user.get().getName());

        return ResponseEntity.status(200)
                .body(new ApiResponse(true, "Login successful", loginResponse));
    }

    public ResponseEntity<?> sendForgotPasswordOtp(ForgotPasswordRequest request){
        Optional<AuthEntity> user = authRepository.findByEmail(request.getEmail());
        if(user.isEmpty()){
            return ResponseEntity.status(404)
                    .body(new ApiResponse(false, "No account found with this email address", null));
        }
        Integer otp = otpService.generateAndSaveOtp(user.get().getEmail());
        mailService.sendProfessionalOtpEmail(user.get().getEmail(),otp);

        return ResponseEntity.status(200)
                .body(new ApiResponse(true, "OTP sent successfully", null));

    }

    public ResponseEntity<?> verifyForgotPasswordOtp(ForgotPasswordOtpVerifyRequest request) {
        return otpService.verifyOtpOnly(request);
    }

    public ResponseEntity<?> resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.status(422).body(new ApiResponse(false, "New password and confirm password do not match", null));
        }

        Optional<AuthEntity> user = authRepository.findByEmail(request.getEmail());

        if (user.isEmpty()) {
            return ResponseEntity.status(404).body(new ApiResponse(false, "No account found with this email address", null));
        }
        user.get().setPassword(passwordEncoder.encode(request.getNewPassword()));
        authRepository.save(user.get());

        return ResponseEntity.status(200).body(new ApiResponse(true, "Password reset successfully", null));
    }

}

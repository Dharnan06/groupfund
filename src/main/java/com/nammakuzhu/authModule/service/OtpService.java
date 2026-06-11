package com.nammakuzhu.authModule.service;

import com.nammakuzhu.authModule.dto.ApiResponse;
import com.nammakuzhu.authModule.dto.ForgotPasswordOtpVerifyRequest;
import com.nammakuzhu.authModule.dto.LoginResponse;
import com.nammakuzhu.authModule.dto.OtpVerifyRequest;
import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.authModule.entity.OtpEntity;
import com.nammakuzhu.authModule.repository.AuthRepository;
import com.nammakuzhu.authModule.repository.OtpRepository;
import com.nammakuzhu.profileModule.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.nammakuzhu.profileModule.entity.ProfileEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    private final OtpRepository otpRepository ;
    private final AuthRepository authRepository;
    private final JwtService jwtService;
    private final ProfileRepository profileRepository;

    OtpService(OtpRepository otpRepository,
               AuthRepository authRepository,
               JwtService jwtService,
               ProfileRepository profileRepository) {
        this.otpRepository = otpRepository;
        this.authRepository = authRepository;
        this.jwtService = jwtService;
        this.profileRepository = profileRepository;
    }
    public Integer generateAndSaveOtp(String email){
        OtpEntity otpEntity = new OtpEntity();
        int otp = new Random().nextInt(900000)+100000;
        otpEntity.setOtp(otp);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpEntity.setUsed(false);
        otpEntity.setEmail(email);
        otpRepository.save(otpEntity);
        return otp;
    }
    public ResponseEntity<?> verifyOtp(OtpVerifyRequest request){
        Optional<OtpEntity> otpInfo = otpRepository.findTopByEmailOrderByIdDesc(request.getEmail());
        if (otpInfo.isEmpty()) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "Verify you Email once again", null));
        }
        if(otpInfo.get().isUsed()){
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "You already used this OTP , Request new otp", null));
        }
        if(!request.getOtp().equals(otpInfo.get().getOtp())){
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "You entered an invalid OTP", null));
        }
        if (LocalDateTime.now().isAfter(otpInfo.get().getExpiryTime())) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "OTP expired. Please request a new OTP", null));
        }
        otpInfo.get().setUsed(true);
        otpRepository.save(otpInfo.get());

        Optional<AuthEntity> user = authRepository.findByEmail(request.getEmail());
        if(user.isEmpty()){
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "Verify you Email once again", null));
        }
        user.get().setEmailVerified(true);
        authRepository.save(user.get());
        if (!profileRepository.existsByUser(user.get())) {
            ProfileEntity profile = new ProfileEntity();
            profile.setUser(user.get());
            profile.setFullName(user.get().getName());
            profile.setProfileCompletionPercentage(7);
            profile.setProfileCompleted(false);
            profileRepository.save(profile);
        }
        String token = jwtService.generateToken(request.getEmail());


        return ResponseEntity.status(200)
                .body(new ApiResponse(
                        true,
                        "OTP verified successfully",
                        new LoginResponse(token, user.get().getEmail(), user.get().getId(), user.get().getName())
                ));
    }

    public ResponseEntity<?> verifyOtpOnly(ForgotPasswordOtpVerifyRequest forgotPasswordOtpVerifyRequest){
        Optional<OtpEntity> otpInfo = otpRepository.findTopByEmailOrderByIdDesc(forgotPasswordOtpVerifyRequest.getEmail());
        if (otpInfo.isEmpty()) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "Verify you Email once again", null));
        }
        if(otpInfo.get().isUsed()){
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "You already used this OTP , Request new otp", null));
        }
        if(!forgotPasswordOtpVerifyRequest.getOtp().equals(otpInfo.get().getOtp())){
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "You entered an invalid OTP", null));
        }
        if (LocalDateTime.now().isAfter(otpInfo.get().getExpiryTime())) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "OTP expired. Please request a new OTP", null));
        }
        otpInfo.get().setUsed(true);
        otpRepository.save(otpInfo.get());
        return ResponseEntity.status(200)
                .body(new ApiResponse(true, "OTP verified successfully", null));
    }
}

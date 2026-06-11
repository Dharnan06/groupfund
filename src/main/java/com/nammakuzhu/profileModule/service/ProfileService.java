package com.nammakuzhu.profileModule.service;

import com.nammakuzhu.authModule.dto.ApiResponse;
import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.authModule.repository.AuthRepository;
import com.nammakuzhu.profileModule.dto.ProfileRequest;
import com.nammakuzhu.profileModule.dto.ProfileResponse;
import com.nammakuzhu.profileModule.entity.ProfileEntity;
import com.nammakuzhu.profileModule.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.Map;
import java.util.Optional;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final AuthRepository authRepository;
    private final Cloudinary cloudinary;

    public ProfileService(ProfileRepository profileRepository,
                          AuthRepository authRepository,
                          Cloudinary cloudinary) {
        this.profileRepository = profileRepository;
        this.authRepository = authRepository;
        this.cloudinary = cloudinary;
    }

    private AuthEntity getLoggedInUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return authRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    public ResponseEntity<?> createOrUpdateProfile(ProfileRequest request) {
        AuthEntity user = getLoggedInUser();

        ProfileEntity profile = profileRepository.findByUser(user)
                .orElse(new ProfileEntity());

        profile.setUser(user);
        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
            user.setName(request.getFullName());
            authRepository.save(user);
        }
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) profile.setGender(request.getGender());

        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getDistrict() != null) profile.setDistrict(request.getDistrict());
        if (request.getState() != null) profile.setState(request.getState());
        if (request.getPincode() != null) profile.setPincode(request.getPincode());

        if (request.getFatherName() != null) profile.setFatherName(request.getFatherName());
        if (request.getMotherName() != null) profile.setMotherName(request.getMotherName());
        if (request.getOccupation() != null) profile.setOccupation(request.getOccupation());
        if (request.getMonthlyIncome() != null) profile.setMonthlyIncome(request.getMonthlyIncome());
        if (request.getMaritalStatus() != null) profile.setMaritalStatus(request.getMaritalStatus());
        if (request.getAadhaarNumber() != null) profile.setAadhaarNumber(request.getAadhaarNumber());

        int percentage = calculateProfileCompletion(profile);
        profile.setProfileCompletionPercentage(percentage);
        profile.setProfileCompleted(percentage == 100);

        profileRepository.save(profile);

        return ResponseEntity.status(200)
                .body(new ApiResponse(true, "Profile saved successfully", toResponse(profile)));
    }

    public ResponseEntity<?> getMyProfile() {
        AuthEntity user = getLoggedInUser();

        Optional<ProfileEntity> profile = profileRepository.findByUser(user);

        if (profile.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse(false, "Profile not created yet", null));
        }

        return ResponseEntity.status(200)
                .body(new ApiResponse(true, "Profile fetched successfully", toResponse(profile.get())));
    }

    private int calculateProfileCompletion(ProfileEntity profile) {
        int total = 13;
        int completed = 0;

        if (profile.getFullName() != null && !profile.getFullName().isBlank()) completed++;
        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isBlank()) completed++;
        if (profile.getDateOfBirth() != null) completed++;
        if (profile.getGender() != null && !profile.getGender().isBlank()) completed++;
        if (profile.getAddress() != null && !profile.getAddress().isBlank()) completed++;
        if (profile.getDistrict() != null && !profile.getDistrict().isBlank()) completed++;
        if (profile.getState() != null && !profile.getState().isBlank()) completed++;
        if (profile.getPincode() != null && !profile.getPincode().isBlank()) completed++;
        if (profile.getFatherName() != null && !profile.getFatherName().isBlank()) completed++;
        if (profile.getMotherName() != null && !profile.getMotherName().isBlank()) completed++;
        if (profile.getOccupation() != null && !profile.getOccupation().isBlank()) completed++;
        if (profile.getMonthlyIncome() != null) completed++;
        if (profile.getMaritalStatus() != null && !profile.getMaritalStatus().isBlank()) completed++;

        return (completed * 100) / total;
    }

    private ProfileResponse toResponse(ProfileEntity profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getFullName(),
                profile.getUser().getEmail(),
                profile.getUser().getMobileNumber(),
                profile.getProfileImageUrl(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getAddress(),
                profile.getDistrict(),
                profile.getState(),
                profile.getPincode(),
                profile.getFatherName(),
                profile.getMotherName(),
                profile.getOccupation(),
                profile.getMonthlyIncome(),
                profile.getMaritalStatus(),
                profile.getAadhaarNumber(),
                profile.isAadhaarNumberVerified(),
                profile.isProfileCompleted(),
                profile.getProfileCompletionPercentage()
        );
    }

    public ResponseEntity<?> uploadProfileImage(MultipartFile file) {
        try {
            AuthEntity user = getLoggedInUser();

            ProfileEntity profile = profileRepository.findByUser(user)
                    .orElse(new ProfileEntity());

            profile.setUser(user);

            String publicId = "groupfund/profiles/user_" + user.getId();

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "groupfund/profiles",
                            "public_id", "user_" + user.getId(),
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );

            String imageUrl = uploadResult.get("secure_url").toString();

            profile.setProfileImageUrl(imageUrl);

            int percentage = calculateProfileCompletion(profile);
            profile.setProfileCompletionPercentage(percentage);
            profile.setProfileCompleted(percentage == 100);

            profileRepository.save(profile);

            return ResponseEntity.status(200)
                    .body(new ApiResponse(true, "Profile image uploaded successfully", toResponse(profile)));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "Failed to upload profile image: " + e.getMessage(), null));
        }
    }

    public ResponseEntity<?> removeProfileImage() {
        try {
            AuthEntity user = getLoggedInUser();

            ProfileEntity profile = profileRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Profile not found"));

            String publicId = "groupfund/profiles/user_" + user.getId();

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            profile.setProfileImageUrl(null);

            int percentage = calculateProfileCompletion(profile);
            profile.setProfileCompletionPercentage(percentage);
            profile.setProfileCompleted(percentage == 100);

            profileRepository.save(profile);

            return ResponseEntity.ok(
                    new ApiResponse(true, "Profile image removed successfully", toResponse(profile))
            );

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "Failed to remove profile image: " + e.getMessage(), null));
        }
    }

}
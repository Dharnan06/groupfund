package com.nammakuzhu.profileModule.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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

    private ProfileEntity getOrCreateProfile(AuthEntity user) {
        ProfileEntity profile = profileRepository.findByUserWithAuth(user)
                .orElse(new ProfileEntity());

        if (profile.getUser() == null) {
            profile.setUser(user);
        }

        return profile;
    }

    @Transactional
    public ResponseEntity<?> createOrUpdateProfile(ProfileRequest request) {
        AuthEntity user = getLoggedInUser();
        ProfileEntity profile = getOrCreateProfile(user);

        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());

            if (!request.getFullName().equals(user.getName())) {
                user.setName(request.getFullName());
            }
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

        updateProfileCompletion(profile);

        ProfileEntity savedProfile = profileRepository.save(profile);

        return ResponseEntity.ok(
                new ApiResponse(true, "Profile saved successfully", toResponse(savedProfile))
        );
    }

    public ResponseEntity<?> getMyProfile() {
        AuthEntity user = getLoggedInUser();

        ProfileEntity profile = profileRepository.findByUserWithAuth(user)
                .orElse(null);

        if (profile == null) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse(false, "Profile not created yet", null));
        }

        return ResponseEntity.ok(
                new ApiResponse(true, "Profile fetched successfully", toResponse(profile))
        );
    }

    @Transactional
    public ResponseEntity<?> uploadProfileImage(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Please choose an image", null));
            }

            if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Only image files are allowed", null));
            }

            AuthEntity user = getLoggedInUser();
            ProfileEntity profile = getOrCreateProfile(user);

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "groupfund/profiles",
                            "public_id", "user_" + user.getId(),
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );

            String imageUrl = String.valueOf(uploadResult.get("secure_url"));

            if (imageUrl.startsWith("https//")) {
                imageUrl = imageUrl.replace("https//", "https://");
            }

            profile.setProfileImageUrl(imageUrl);

            updateProfileCompletion(profile);

            ProfileEntity savedProfile = profileRepository.save(profile);

            return ResponseEntity.ok(
                    new ApiResponse(true, "Profile image uploaded successfully", toResponse(savedProfile))
            );

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "Failed to upload profile image: " + e.getMessage(), null));
        }
    }

    @Transactional
    public ResponseEntity<?> removeProfileImage() {
        try {
            AuthEntity user = getLoggedInUser();

            ProfileEntity profile = profileRepository.findByUserWithAuth(user)
                    .orElseThrow(() -> new RuntimeException("Profile not found"));

            String publicId = "groupfund/profiles/user_" + user.getId();

            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            profile.setProfileImageUrl(null);

            updateProfileCompletion(profile);

            ProfileEntity savedProfile = profileRepository.save(profile);

            return ResponseEntity.ok(
                    new ApiResponse(true, "Profile image removed successfully", toResponse(savedProfile))
            );

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "Failed to remove profile image: " + e.getMessage(), null));
        }
    }

    private void updateProfileCompletion(ProfileEntity profile) {
        int percentage = calculateProfileCompletion(profile);
        profile.setProfileCompletionPercentage(percentage);
        profile.setProfileCompleted(percentage == 100);
    }

    private int calculateProfileCompletion(ProfileEntity profile) {
        int total = 13;
        int completed = 0;

        if (isFilled(profile.getFullName())) completed++;
        if (isFilled(profile.getProfileImageUrl())) completed++;
        if (profile.getDateOfBirth() != null) completed++;
        if (isFilled(profile.getGender())) completed++;
        if (isFilled(profile.getAddress())) completed++;
        if (isFilled(profile.getDistrict())) completed++;
        if (isFilled(profile.getState())) completed++;
        if (isFilled(profile.getPincode())) completed++;
        if (isFilled(profile.getFatherName())) completed++;
        if (isFilled(profile.getMotherName())) completed++;
        if (isFilled(profile.getOccupation())) completed++;
        if (profile.getMonthlyIncome() != null) completed++;
        if (isFilled(profile.getMaritalStatus())) completed++;

        return (completed * 100) / total;
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private ProfileResponse toResponse(ProfileEntity profile) {
        AuthEntity user = profile.getUser();

        return new ProfileResponse(
                profile.getId(),
                profile.getFullName(),
                user.getEmail(),
                user.getMobileNumber(),
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
}
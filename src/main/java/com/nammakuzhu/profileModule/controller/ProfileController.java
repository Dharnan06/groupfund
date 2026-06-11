package com.nammakuzhu.profileModule.controller;

import com.nammakuzhu.profileModule.dto.ProfileRequest;
import com.nammakuzhu.profileModule.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveProfile(@RequestBody ProfileRequest request) {
        return profileService.createOrUpdateProfile(request);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        return profileService.getMyProfile();
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        return profileService.uploadProfileImage(file);
    }

    @DeleteMapping("/remove-image")
    public ResponseEntity<?> removeProfileImage() {
        return profileService.removeProfileImage();
    }

}
package com.nammakuzhu.profileModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.profileModule.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    Optional<ProfileEntity> findByUser(AuthEntity user);

    boolean existsByUser(AuthEntity user);

    List<ProfileEntity> findByFullNameContainingIgnoreCaseOrUser_MobileNumberContaining(
            String fullName,
            String mobileNumber
    );
}
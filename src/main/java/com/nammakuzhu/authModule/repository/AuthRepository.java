package com.nammakuzhu.authModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<AuthEntity,Long> {
    boolean existsByEmail(String email);
    boolean existsByMobileNumber(String mobileNumber);
    Optional<AuthEntity> findByEmail(String email);
    Optional<AuthEntity> findByMobileNumber(String mobileNumber);
}

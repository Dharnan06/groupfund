package com.nammakuzhu.profileModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.profileModule.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {

    Optional<ProfileEntity> findByUser(AuthEntity user);

    boolean existsByUser(AuthEntity user);

    List<ProfileEntity> findByFullNameContainingIgnoreCaseOrUser_MobileNumberContaining(
            String fullName,
            String mobileNumber
    );

    List<ProfileEntity> findByUserIn(List<AuthEntity> users);

    @Query("""
        SELECT p
        FROM ProfileEntity p
        JOIN FETCH p.user
        WHERE p.user = :user
    """)
    Optional<ProfileEntity> findByUserWithAuth(@Param("user") AuthEntity user);

    @Query("""
        SELECT p
        FROM ProfileEntity p
        JOIN FETCH p.user
        WHERE p.user IN :users
    """)
    List<ProfileEntity> findByUserInWithAuth(@Param("users") List<AuthEntity> users);
}
package com.nammakuzhu.friendModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.friendModule.entity.FriendRequestEntity;
import com.nammakuzhu.friendModule.entity.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, Long> {

    boolean existsBySenderAndReceiver(AuthEntity sender, AuthEntity receiver);

    Optional<FriendRequestEntity> findBySenderAndReceiver(AuthEntity sender, AuthEntity receiver);

    List<FriendRequestEntity> findByReceiverAndStatus(AuthEntity receiver, FriendRequestStatus status);

    List<FriendRequestEntity> findBySenderAndStatus(AuthEntity sender, FriendRequestStatus status);

    Optional<FriendRequestEntity> findByIdAndReceiver(
            Long id,
            AuthEntity receiver
    );

}
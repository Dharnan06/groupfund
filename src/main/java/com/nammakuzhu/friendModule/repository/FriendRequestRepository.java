package com.nammakuzhu.friendModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.friendModule.entity.FriendRequestEntity;
import com.nammakuzhu.friendModule.entity.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, Long> {

    boolean existsBySenderAndReceiver(AuthEntity sender, AuthEntity receiver);

    Optional<FriendRequestEntity> findBySenderAndReceiver(AuthEntity sender, AuthEntity receiver);

    List<FriendRequestEntity> findByReceiverAndStatus(AuthEntity receiver, FriendRequestStatus status);

    List<FriendRequestEntity> findBySenderAndStatus(AuthEntity sender, FriendRequestStatus status);

    Optional<FriendRequestEntity> findByIdAndReceiver(Long id, AuthEntity receiver);

    @Query("""
        SELECT fr
        FROM FriendRequestEntity fr
        JOIN FETCH fr.sender
        JOIN FETCH fr.receiver
        WHERE 
            (fr.sender = :userOne AND fr.receiver = :userTwo)
            OR
            (fr.sender = :userTwo AND fr.receiver = :userOne)
    """)
    Optional<FriendRequestEntity> findRequestBetweenUsers(
            @Param("userOne") AuthEntity userOne,
            @Param("userTwo") AuthEntity userTwo
    );

    @Query("""
        SELECT fr
        FROM FriendRequestEntity fr
        JOIN FETCH fr.sender
        JOIN FETCH fr.receiver
        WHERE 
            (fr.sender = :loggedInUser AND fr.receiver IN :users)
            OR
            (fr.receiver = :loggedInUser AND fr.sender IN :users)
    """)
    List<FriendRequestEntity> findRequestsBetweenUserAndUsers(
            @Param("loggedInUser") AuthEntity loggedInUser,
            @Param("users") List<AuthEntity> users
    );

    @Query("""
        SELECT fr
        FROM FriendRequestEntity fr
        JOIN FETCH fr.sender
        JOIN FETCH fr.receiver
        WHERE fr.receiver = :receiver
        AND fr.status = :status
        ORDER BY fr.createdAt DESC
    """)
    List<FriendRequestEntity> findReceivedRequestsWithUsers(
            @Param("receiver") AuthEntity receiver,
            @Param("status") FriendRequestStatus status
    );

    @Query("""
        SELECT fr
        FROM FriendRequestEntity fr
        JOIN FETCH fr.sender
        JOIN FETCH fr.receiver
        WHERE 
            (fr.sender = :user OR fr.receiver = :user)
            AND fr.status = :status
        ORDER BY fr.createdAt DESC
    """)
    List<FriendRequestEntity> findAllByUserAndStatusWithUsers(
            @Param("user") AuthEntity user,
            @Param("status") FriendRequestStatus status
    );
}
package com.nammakuzhu.friendModule.entity;

import com.nammakuzhu.authModule.entity.AuthEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "friend_requests")
public class FriendRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AuthEntity sender;

    @ManyToOne
    private AuthEntity receiver;

    @Enumerated(EnumType.STRING)
    private FriendRequestStatus status;

    private LocalDateTime createdAt;
}
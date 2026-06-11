package com.nammakuzhu.groupModule.entity;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.groupModule.enums.MemberRole;
import com.nammakuzhu.groupModule.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "group_members")
public class GroupMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private GroupEntity group;

    @ManyToOne
    private AuthEntity user;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private LocalDateTime invitedAt;

    private LocalDateTime joinedAt;

    private Boolean activationApproved = false;
}

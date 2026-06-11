package com.nammakuzhu.groupModule.entity;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.groupModule.enums.GroupStatus;
import com.nammakuzhu.groupModule.enums.GroupType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "kuzhu_groups")
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;

    @Enumerated(EnumType.STRING)
    private GroupType groupType;

    @Enumerated(EnumType.STRING)
    private GroupStatus groupStatus;

    @ManyToOne
    @JoinColumn(name = "leader_id")
    private AuthEntity leader;

    private Integer totalMembers;

    /**
     * Maximum members planned by the creator. User can choose 5 to 20.
     * Payments are allowed only after group activation, not immediately after creation.
     */
    private Integer targetMembers;

    private LocalDateTime activationRequestedAt;

    private LocalDate startDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public GroupEntity() {
    }

    // getters and setters
}
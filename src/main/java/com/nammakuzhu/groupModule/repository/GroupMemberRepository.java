package com.nammakuzhu.groupModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.groupModule.entity.GroupMemberEntity;
import com.nammakuzhu.groupModule.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, Long> {

    List<GroupMemberEntity> findByUser(AuthEntity user);

    List<GroupMemberEntity> findByGroup(GroupEntity group);

    long countByGroupAndStatus(GroupEntity group, MemberStatus status);

    boolean existsByGroupAndUser(GroupEntity group, AuthEntity user);

    List<GroupMemberEntity> findByUserAndStatus(AuthEntity user, MemberStatus status);

    long countByGroupAndStatusIn(GroupEntity group, List<MemberStatus> statuses);

    @Query("""
        SELECT gm
        FROM GroupMemberEntity gm
        JOIN FETCH gm.group g
        JOIN FETCH g.leader
        WHERE gm.user = :user
        AND gm.status = :status
        ORDER BY g.createdAt DESC
    """)
    List<GroupMemberEntity> findByUserAndStatusWithGroupAndLeader(
            @Param("user") AuthEntity user,
            @Param("status") MemberStatus status
    );

    @Query("""
        SELECT gm
        FROM GroupMemberEntity gm
        JOIN FETCH gm.user
        WHERE gm.group = :group
        ORDER BY gm.role ASC, gm.joinedAt ASC
    """)
    List<GroupMemberEntity> findByGroupWithUser(
            @Param("group") GroupEntity group
    );

    @Query("""
        SELECT gm
        FROM GroupMemberEntity gm
        JOIN FETCH gm.group g
        JOIN FETCH g.leader
        WHERE gm.user = :user
        AND gm.status = :status
    """)
    List<GroupMemberEntity> findAcceptedGroupsForDashboard(
            @Param("user") AuthEntity user,
            @Param("status") MemberStatus status
    );
}
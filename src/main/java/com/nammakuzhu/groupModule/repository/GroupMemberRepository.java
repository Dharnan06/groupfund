package com.nammakuzhu.groupModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.groupModule.entity.GroupMemberEntity;
import com.nammakuzhu.groupModule.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, Long> {

    List<GroupMemberEntity> findByUser(AuthEntity user);

    List<GroupMemberEntity> findByGroup(GroupEntity group);

    long countByGroupAndStatus(GroupEntity group, MemberStatus status);

    boolean existsByGroupAndUser(GroupEntity group, AuthEntity user);

    List<GroupMemberEntity> findByUserAndStatus(AuthEntity user, MemberStatus status);


}
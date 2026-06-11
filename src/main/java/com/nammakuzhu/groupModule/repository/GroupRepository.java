package com.nammakuzhu.groupModule.repository;

import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.groupModule.enums.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    List<GroupEntity> findByLeader(AuthEntity leader);

    long countByLeader(AuthEntity leader);

    long countByLeaderAndGroupStatus(AuthEntity leader, GroupStatus groupStatus);
}
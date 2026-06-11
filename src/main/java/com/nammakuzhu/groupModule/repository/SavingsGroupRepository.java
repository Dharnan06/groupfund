package com.nammakuzhu.groupModule.repository;

import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.groupModule.entity.SavingsGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavingsGroupRepository extends JpaRepository<SavingsGroupEntity, Long> {
    Optional<SavingsGroupEntity> findByGroup(GroupEntity group);
    void deleteByGroup(GroupEntity group);
}

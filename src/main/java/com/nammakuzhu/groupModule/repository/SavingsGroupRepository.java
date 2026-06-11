package com.nammakuzhu.groupModule.repository;

import com.nammakuzhu.groupModule.entity.GroupEntity;
import com.nammakuzhu.groupModule.entity.SavingsGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SavingsGroupRepository extends JpaRepository<SavingsGroupEntity, Long> {

    Optional<SavingsGroupEntity> findByGroup(GroupEntity group);

    void deleteByGroup(GroupEntity group);

    @Query("""
        SELECT COALESCE(SUM(s.monthlySavingsAmount), 0)
        FROM SavingsGroupEntity s
        WHERE s.group IN :groups
    """)
    BigDecimal sumMonthlySavingsByGroups(@Param("groups") List<GroupEntity> groups);
}
package com.nammakuzhu.groupModule.repository;

import com.nammakuzhu.groupModule.entity.CustomLoanEntity;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomLoanRepository extends JpaRepository<CustomLoanEntity, Long> {

    Optional<CustomLoanEntity> findByGroup(GroupEntity group);

    void deleteByGroup(GroupEntity group);

    @Query("""
        SELECT COALESCE(SUM(c.loanAmount), 0)
        FROM CustomLoanEntity c
        WHERE c.group IN :groups
    """)
    BigDecimal sumLoanAmountByGroups(@Param("groups") List<GroupEntity> groups);
}
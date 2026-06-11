package com.nammakuzhu.groupModule.repository;

import com.nammakuzhu.groupModule.entity.CustomLoanEntity;
import com.nammakuzhu.groupModule.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomLoanRepository extends JpaRepository<CustomLoanEntity, Long> {
    Optional<CustomLoanEntity> findByGroup(GroupEntity group);
    void deleteByGroup(GroupEntity group);
}

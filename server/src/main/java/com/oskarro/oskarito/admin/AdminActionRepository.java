package com.oskarro.oskarito.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminActionRepository extends JpaRepository<AdminAction, Long> {

    List<AdminAction> findTop200ByOrderByCreatedAtDesc();

    List<AdminAction> findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Integer targetId);
}

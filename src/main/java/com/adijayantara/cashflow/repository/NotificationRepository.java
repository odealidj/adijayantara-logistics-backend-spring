package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserIdOrTargetRoleOrderByCreatedAtDesc(UUID userId, String targetRole, Pageable pageable);
    long countByUserIdAndIsReadFalse(UUID userId);
    long countByTargetRoleAndIsReadFalse(String targetRole);
}


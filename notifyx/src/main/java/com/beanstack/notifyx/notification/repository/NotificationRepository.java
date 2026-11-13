package com.beanstack.notifyx.notification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.beanstack.notifyx.notification.entity.Notification;
import jakarta.persistence.LockModeType;

public interface NotificationRepository
    extends JpaRepository<Notification, UUID> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<Notification> findByProject_IdAndIdempotencyKey(
        UUID projectId,
        String idempotencyKey
    );
}
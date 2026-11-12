package com.beanstack.notifyx.notification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanstack.notifyx.notification.entity.Notification;

public interface NotificationRepository
    extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByProject_IdAndIdempotencyKey(
        UUID projectId,
        String idempotencyKey
    );
}
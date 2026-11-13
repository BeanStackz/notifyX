package com.beanstack.notifyx.notification.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.beanstack.notifyx.notification.entity.Notification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository
    extends JpaRepository<Notification, UUID> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<Notification> findByProject_IdAndIdempotencyKey(
        UUID projectId,
        String idempotencyKey
    );

    List<Notification> findByProject_IdOrderByCreatedAtDesc(UUID projectId);

    @Query("""
        select n.status, count(n)
        from Notification n
        where n.project.organization.id = :organizationId
        group by n.status
        """)
    List<Object[]> countByStatusForOrganization(@Param("organizationId") UUID organizationId);

    @Query("""
        select n.channel, count(n)
        from Notification n
        where n.project.organization.id = :organizationId
        group by n.channel
        """)
    List<Object[]> countByChannelForOrganization(@Param("organizationId") UUID organizationId);

    long countByProject_Organization_Id(UUID organizationId);
}
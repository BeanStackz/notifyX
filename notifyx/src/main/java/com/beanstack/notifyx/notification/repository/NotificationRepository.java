package com.beanstack.notifyx.notification.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

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
        select n
        from Notification n
        join fetch n.project p
        join fetch p.organization
        where n.id = :notificationId
        """)
    Optional<Notification> findWithProjectAndOrganizationById(
        @Param("notificationId") UUID notificationId
    );

    @Query("""
        select n
        from Notification n
        join fetch n.project
        where n.project.id = :projectId
        order by n.createdAt desc
        """)
    List<Notification> findAllWithProjectByProjectIdOrderByCreatedAtDesc(
        @Param("projectId") UUID projectId
    );

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

    @Query(value = """
        select cast(n.created_at as date) as notification_day, count(*) as notification_count
        from notifications n
        join projects p on p.id = n.project_id
        where p.organization_id = :organizationId
          and n.created_at >= :from
          and n.created_at < :to
        group by cast(n.created_at as date)
        order by notification_day
        """, nativeQuery = true)
    List<Object[]> countDailyForOrganization(
        @Param("organizationId") UUID organizationId,
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}
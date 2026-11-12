package com.beanstack.notifyx.subscription.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanstack.notifyx.subscription.entity.SubscriptionUsage;

import jakarta.persistence.LockModeType;

public interface SubscriptionUsageRepository
    extends JpaRepository<SubscriptionUsage, UUID> {

    Optional<SubscriptionUsage>
    findBySubscription_IdAndPeriodStart(
        UUID subscriptionId,
        Instant periodStart
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM SubscriptionUsage u
        WHERE u.subscription.id = :subscriptionId
          AND u.periodStart = :periodStart
    """)
    Optional<SubscriptionUsage> findForUpdate(
        @Param("subscriptionId") UUID subscriptionId,
        @Param("periodStart") Instant periodStart
    );
}
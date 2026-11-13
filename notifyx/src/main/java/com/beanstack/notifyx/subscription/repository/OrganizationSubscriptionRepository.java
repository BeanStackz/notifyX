package com.beanstack.notifyx.subscription.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.beanstack.notifyx.subscription.entity.OrganizationSubscription;
import com.beanstack.notifyx.subscription.entity.SubscriptionStatus;

import jakarta.persistence.LockModeType;

public interface OrganizationSubscriptionRepository
    extends JpaRepository<OrganizationSubscription, UUID> {

    Optional<OrganizationSubscription>
    findFirstByOrganization_IdAndStatusOrderByCreatedAtDesc(
        UUID organizationId,
        SubscriptionStatus status
    );

    Optional<OrganizationSubscription>
    findFirstByOrganization_IdAndStatusInOrderByCreatedAtDesc(
        UUID organizationId,
        Collection<SubscriptionStatus> statuses
    );

    Optional<OrganizationSubscription>
    findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    Optional<OrganizationSubscription>
    findByIdAndOrganization_Id(UUID id, UUID organizationId);

    List<OrganizationSubscription>
    findAllByOrganization_IdOrderByCreatedAtDesc(UUID organizationId);

    List<OrganizationSubscription>
    findAllByStatusInAndCurrentPeriodEndLessThanEqual(
        Collection<SubscriptionStatus> statuses,
        Instant currentPeriodEnd
    );

    boolean existsByOrganization_IdAndStatusIn(
        UUID organizationId,
        Collection<SubscriptionStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM OrganizationSubscription s
        WHERE s.id = :id
    """)
    Optional<OrganizationSubscription> findByIdForUpdate(
        @Param("id") UUID id
    );
}
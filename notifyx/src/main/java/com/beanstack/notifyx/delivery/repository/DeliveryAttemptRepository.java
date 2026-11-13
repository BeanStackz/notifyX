package com.beanstack.notifyx.delivery.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beanstack.notifyx.delivery.entity.DeliveryAttempt;

public interface DeliveryAttemptRepository
    extends JpaRepository<DeliveryAttempt, UUID> {
    Optional<DeliveryAttempt> findFirstByNotification_IdOrderByAttemptNumberDesc(
        UUID notificationId
    );

    List<DeliveryAttempt> findAllByNotification_IdOrderByAttemptNumberAsc(UUID notificationId);

    Optional<DeliveryAttempt> findFirstByProviderMessageId(String providerMessageId);
}

package com.beanstack.notifyx.subscription.service;

import java.time.Instant;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.subscription.entity.SubscriptionStatus;
import com.beanstack.notifyx.subscription.repository.OrganizationSubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SubscriptionExpiryJob {

    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final DefaultSubscriptionProvisioner freeProvisioner;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireSubscriptions() {
        var expiredSubscriptions =
            subscriptionRepository
                .findAllByStatusInAndCurrentPeriodEndLessThanEqual(
                    Set.of(
                        SubscriptionStatus.ACTIVE,
                        SubscriptionStatus.PAST_DUE
                    ),
                    Instant.now()
                );

        for (var subscription : expiredSubscriptions) {
            boolean shouldDowngrade =
                subscription.isCancelAtPeriodEnd() ||
                subscription.getStatus() == SubscriptionStatus.PAST_DUE;

            if (!shouldDowngrade) {
                continue;
            }

            subscription.setStatus(SubscriptionStatus.EXPIRED);
            freeProvisioner.activateFreePlan(
                subscription.getOrganization()
            );
        }
    }
}

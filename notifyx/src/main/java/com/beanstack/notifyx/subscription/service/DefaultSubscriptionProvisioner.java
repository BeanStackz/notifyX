package com.beanstack.notifyx.subscription.service;

import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beanstack.notifyx.organization.entity.Organization;
import com.beanstack.notifyx.subscription.entity.OrganizationSubscription;
import com.beanstack.notifyx.subscription.entity.Plan;
import com.beanstack.notifyx.subscription.entity.SubscriptionStatus;
import com.beanstack.notifyx.subscription.entity.SubscriptionUsage;
import com.beanstack.notifyx.subscription.repository.OrganizationSubscriptionRepository;
import com.beanstack.notifyx.subscription.repository.PlanRepository;
import com.beanstack.notifyx.subscription.repository.SubscriptionUsageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultSubscriptionProvisioner {

    private final PlanRepository planRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionUsageRepository usageRepository;

    @Transactional
    public OrganizationSubscription assignFreePlan(Organization organization) {

        Plan freePlan = planRepository.findByCodeAndEnabledTrue("FREE")
            .orElseThrow(() ->
                new IllegalStateException("FREE plan is not configured")
            );

        Instant now = Instant.now();
        Instant periodEnd = now.atZone(ZoneOffset.UTC)
            .plusMonths(1)
            .toInstant();

        OrganizationSubscription subscription =
            OrganizationSubscription.builder()
                .organization(organization)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now)
                .currentPeriodEnd(periodEnd)
                .cancelAtPeriodEnd(false)
                .build();

        OrganizationSubscription saved =
            subscriptionRepository.saveAndFlush(subscription);

        SubscriptionUsage usage = SubscriptionUsage.builder()
            .subscription(saved)
            .periodStart(now)
            .periodEnd(periodEnd)
            .notificationsUsed(0)
            .build();

        usageRepository.save(usage);

        return saved;
    }

    @Transactional
public OrganizationSubscription activateFreePlan(
    Organization organization
) {
    Plan freePlan = planRepository.findByCodeAndEnabledTrue("FREE")
        .orElseThrow(() ->
            new IllegalStateException("FREE plan is not configured")
        );

    Instant now = Instant.now();
    Instant end = now.atZone(ZoneOffset.UTC)
        .plusMonths(1)
        .toInstant();

    OrganizationSubscription subscription =
        OrganizationSubscription.builder()
            .organization(organization)
            .plan(freePlan)
            .status(SubscriptionStatus.ACTIVE)
            .currentPeriodStart(now)
            .currentPeriodEnd(end)
            .cancelAtPeriodEnd(false)
            .build();

    OrganizationSubscription saved =
        subscriptionRepository.saveAndFlush(subscription);

    usageRepository.save(
        SubscriptionUsage.builder()
            .subscription(saved)
            .periodStart(now)
            .periodEnd(end)
            .notificationsUsed(0)
            .build()
    );

    return saved;
    }
}
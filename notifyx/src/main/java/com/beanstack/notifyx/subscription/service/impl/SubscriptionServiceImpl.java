package com.beanstack.notifyx.subscription.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.beanstack.notifyx.identity.repository.UserRepository;
import com.beanstack.notifyx.notification.entity.NotificationChannel;
import com.beanstack.notifyx.notification.entity.NotificationPriority;
import com.beanstack.notifyx.organization.entity.Organization;
import com.beanstack.notifyx.organization.entity.OrganizationMember;
import com.beanstack.notifyx.organization.entity.OrganizationRole;
import com.beanstack.notifyx.organization.repository.OrganizationMemberRepository;
import com.beanstack.notifyx.organization.repository.OrganizationRepository;
import com.beanstack.notifyx.payment.config.RazorpayProperties;
import com.beanstack.notifyx.payment.dto.RazorpaySubscriptionResult;
import com.beanstack.notifyx.payment.service.RazorpayClientService;
import com.beanstack.notifyx.shared.exception.ResourceNotFoundException;
import com.beanstack.notifyx.subscription.dto.request.VerifySubscriptionPaymentRequest;
import com.beanstack.notifyx.subscription.dto.response.PlanResponse;
import com.beanstack.notifyx.subscription.dto.response.SubscriptionCheckoutResponse;
import com.beanstack.notifyx.subscription.dto.response.SubscriptionResponse;
import com.beanstack.notifyx.subscription.entity.OrganizationSubscription;
import com.beanstack.notifyx.subscription.entity.Plan;
import com.beanstack.notifyx.subscription.entity.SubscriptionStatus;
import com.beanstack.notifyx.subscription.entity.SubscriptionUsage;
import com.beanstack.notifyx.subscription.exception.RazorpayCheckoutCreationException;
import com.beanstack.notifyx.subscription.exception.UnsupportedNotificationChannelException;
import com.beanstack.notifyx.subscription.repository.OrganizationSubscriptionRepository;
import com.beanstack.notifyx.subscription.repository.PlanRepository;
import com.beanstack.notifyx.subscription.repository.SubscriptionUsageRepository;
import com.beanstack.notifyx.subscription.service.SubscriptionService;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Set<SubscriptionStatus> PENDING_STATUSES =
        Set.of(
            SubscriptionStatus.PENDING_PAYMENT,
            SubscriptionStatus.AUTHENTICATED
        );

    private final PlanRepository planRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionUsageRepository usageRepository;

    private final RazorpayClientService razorpayClientService;
    private final RazorpayProperties razorpayProperties;

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getPlans() {

        return planRepository.findAllByEnabledTrueOrderByMonthlyQuotaAsc()
            .stream()
            .map(plan -> new PlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getMonthlyPriceInPaise(),
                plan.getCurrency(),
                plan.getMonthlyQuota(),
                plan.getRateLimitPerMinute(),
                plan.getPriority(),
                plan.getAllowedChannels()
            ))
            .toList();
    }

    @Override
    @Transactional(noRollbackFor = RazorpayCheckoutCreationException.class)
    public SubscriptionCheckoutResponse createCheckout(
        UUID organizationId,
        String userEmail,
        String planCode
    ) {

        Organization organization = findOrganization(organizationId);

        requireBillingPermission(organizationId, userEmail);

        Plan plan = planRepository.findByCodeAndEnabledTrue(
            planCode.trim().toUpperCase()
        ).orElseThrow(() ->
            new ResourceNotFoundException("Plan not found")
        );

        if ("FREE".equals(plan.getCode())) {
            throw new IllegalArgumentException(
                "FREE plan is assigned automatically and does not require checkout"
            );
        }

        if (plan.getRazorpayPlanId() == null ||
            plan.getRazorpayPlanId().isBlank()) {
            throw new IllegalStateException(
                "Razorpay plan has not been provisioned"
            );
        }

        boolean pendingExists = subscriptionRepository
            .existsByOrganization_IdAndStatusIn(
                organizationId,
                PENDING_STATUSES
            );

        if (pendingExists) {
            throw new IllegalStateException(
                "An upgrade payment is already pending"
            );
        }

        OrganizationSubscription pending =
            OrganizationSubscription.builder()
                .organization(organization)
                .plan(plan)
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .cancelAtPeriodEnd(false)
                .build();

        OrganizationSubscription saved =
            subscriptionRepository.saveAndFlush(pending);

        try {
            RazorpaySubscriptionResult razorpay =
                razorpayClientService.createSubscription(
                    plan.getRazorpayPlanId(),
                    saved.getId().toString(),
                    organizationId.toString(),
                    120
                );

            saved.setRazorpaySubscriptionId(
                razorpay.subscriptionId()
            );

            return new SubscriptionCheckoutResponse(
                saved.getId(),
                razorpay.subscriptionId(),
                plan.getRazorpayPlanId(),
                razorpayClientService.getKeyId(),
                razorpay.shortUrl(),
                plan.getName(),
                plan.getMonthlyPriceInPaise(),
                plan.getCurrency()
            );

        } catch (Exception exception) {
            saved.setStatus(
                SubscriptionStatus.PAYMENT_CREATION_FAILED
            );

            throw new RazorpayCheckoutCreationException(
                "Unable to create Razorpay subscription",
                exception
            );
        }
    }

    @Override
    @Transactional
    public void verifyCheckout(
        UUID organizationId,
        UUID subscriptionId,
        String userEmail,
        VerifySubscriptionPaymentRequest request
    ) {

        requireBillingPermission(organizationId, userEmail);

        OrganizationSubscription subscription =
            subscriptionRepository
                .findByIdAndOrganization_Id(subscriptionId, organizationId)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Subscription not found")
                );

        if (!PENDING_STATUSES.contains(subscription.getStatus())) {
            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                return;
            }

            throw new IllegalStateException(
                "Subscription is not awaiting payment"
            );
        }

        if (!request.razorpaySubscriptionId().equals(
            subscription.getRazorpaySubscriptionId()
        )) {
            throw new IllegalArgumentException(
                "Razorpay subscription ID mismatch"
            );
        }

        String message = request.razorpayPaymentId()
            + "|"
            + subscription.getRazorpaySubscriptionId();

        String expected = generateHmac(
            message,
            razorpayProperties.keySecret()
        );

        boolean valid = MessageDigest.isEqual(
            HexFormat.of().parseHex(expected),
            HexFormat.of().parseHex(request.razorpaySignature())
        );

        if (!valid) {
            throw new IllegalArgumentException(
                "Invalid Razorpay checkout signature"
            );
        }

        subscription.setAuthorizationPaymentId(
            request.razorpayPaymentId()
        );

        subscription.setStatus(SubscriptionStatus.AUTHENTICATED);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(
        UUID organizationId,
        String userEmail
    ) {

        Organization organization = findOrganization(organizationId);
        requireMembership(organizationId, userEmail);

        OrganizationSubscription subscription =
            findEntitledSubscription(organizationId);

        long used = 0;

        if (subscription.getCurrentPeriodStart() != null) {
            used = usageRepository
                .findBySubscription_IdAndPeriodStart(
                    subscription.getId(),
                    subscription.getCurrentPeriodStart()
                )
                .map(SubscriptionUsage::getNotificationsUsed)
                .orElse(0L);
        }

        long quota = subscription.getPlan().getMonthlyQuota();

        return new SubscriptionResponse(
            subscription.getId(),
            organization.getId(),
            subscription.getPlan().getCode(),
            subscription.getPlan().getName(),
            subscription.getStatus(),
            quota,
            used,
            Math.max(0, quota - used),
            subscription.getCurrentPeriodStart(),
            subscription.getCurrentPeriodEnd()
        );
    }

    @Override
    @Transactional
    public NotificationPriority reserveNotificationQuota(
        UUID organizationId,
        NotificationChannel channel
    ) {

        OrganizationSubscription subscription =
            findEntitledSubscriptionForUpdate(organizationId);

        Plan plan = subscription.getPlan();

        if (!plan.getAllowedChannels().contains(channel)) {
            throw new UnsupportedNotificationChannelException(
                "Your plan does not support channel " + channel
            );
        }

        Instant now = Instant.now();

        Instant periodStart = subscription.getCurrentPeriodStart();
        Instant periodEnd = subscription.getCurrentPeriodEnd();

        if (periodStart == null || periodEnd == null) {
            throw new IllegalStateException(
                "Subscription billing period is missing"
            );
        }

        // FREE subscriptions renew locally each month.
        if ("FREE".equals(plan.getCode()) &&
            !periodEnd.isAfter(now)) {

            periodStart = now;
            periodEnd = now.atZone(ZoneOffset.UTC)
                .plusMonths(1)
                .toInstant();

            subscription.setCurrentPeriodStart(periodStart);
            subscription.setCurrentPeriodEnd(periodEnd);
        }

        if (!periodEnd.isAfter(now)) {
            throw new IllegalStateException(
                "Subscription billing period has expired"
            );
        }

        Instant finalPeriodStart = periodStart;
        Instant finalPeriodEnd = periodEnd;

        SubscriptionUsage usage = usageRepository
            .findForUpdate(subscription.getId(), finalPeriodStart)
            .orElseGet(() -> usageRepository.save(
                SubscriptionUsage.builder()
                    .subscription(subscription)
                    .periodStart(finalPeriodStart)
                    .periodEnd(finalPeriodEnd)
                    .notificationsUsed(0)
                    .build()
            ));

        if (usage.getNotificationsUsed() >= plan.getMonthlyQuota()) {
            throw new IllegalStateException(
                "Monthly notification quota exceeded"
            );
        }

        usage.setNotificationsUsed(
            usage.getNotificationsUsed() + 1
        );

        return plan.getPriority();
    }

    private OrganizationSubscription findEntitledSubscription(
    UUID organizationId
) {
    return subscriptionRepository
        .findFirstByOrganization_IdAndStatusInOrderByCreatedAtDesc(
            organizationId,
            Set.of(
                SubscriptionStatus.ACTIVE,
                SubscriptionStatus.PAST_DUE
            )
        )
        .filter(subscription ->
            subscription.getCurrentPeriodEnd() != null &&
            subscription.getCurrentPeriodEnd().isAfter(Instant.now())
        )
        .orElseThrow(() ->
            new IllegalStateException(
                "Organization does not have an active subscription"
            )
        );
}

    private OrganizationSubscription findEntitledSubscriptionForUpdate(
        UUID organizationId
    ) {
        OrganizationSubscription subscription =
            findEntitledSubscription(organizationId);

        return subscriptionRepository
            .findByIdForUpdate(subscription.getId())
            .orElseThrow(() ->
                new IllegalStateException("Subscription not found")
            );
    }

    private Organization findOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Organization not found")
            );
    }

    private void requireMembership(
        UUID organizationId,
        String email
    ) {
        var user = userRepository.findByEmail(email.toLowerCase().trim())
            .orElseThrow(() ->
                new ResourceNotFoundException("User not found")
            );

        memberRepository.findByOrganizationIdAndUserId(
            organizationId,
            user.getId()
        ).orElseThrow(() ->
            new AccessDeniedException("Organization access denied")
        );
    }

    private void requireBillingPermission(
        UUID organizationId,
        String email
    ) {
        var user = userRepository.findByEmail(email.toLowerCase().trim())
            .orElseThrow(() ->
                new ResourceNotFoundException("User not found")
            );

        OrganizationMember membership =
            memberRepository.findByOrganizationIdAndUserId(
                organizationId,
                user.getId()
            ).orElseThrow(() ->
                new AccessDeniedException("Organization access denied")
            );

        if (membership.getRole() != OrganizationRole.OWNER &&
            membership.getRole() != OrganizationRole.ADMIN) {
            throw new AccessDeniedException(
                "Only organization owners and admins can manage billing"
            );
        }
    }

    private String generateHmac(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            ));

            return HexFormat.of().formatHex(
                mac.doFinal(message.getBytes(StandardCharsets.UTF_8))
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                "Unable to generate Razorpay signature",
                exception
            );
        }
    }
}
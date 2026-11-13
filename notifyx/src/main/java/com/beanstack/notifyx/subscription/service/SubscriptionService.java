package com.beanstack.notifyx.subscription.service;

import java.util.List;
import java.util.UUID;

import com.beanstack.notifyx.notification.entity.NotificationChannel;
import com.beanstack.notifyx.notification.entity.NotificationPriority;
import com.beanstack.notifyx.subscription.dto.request.VerifySubscriptionPaymentRequest;
import com.beanstack.notifyx.subscription.dto.response.PlanResponse;
import com.beanstack.notifyx.subscription.dto.response.SubscriptionCheckoutResponse;
import com.beanstack.notifyx.subscription.dto.response.BillingCheckoutResponse;
import com.beanstack.notifyx.subscription.dto.response.SubscriptionResponse;
import com.beanstack.notifyx.subscription.dto.request.VerifyPaymentRequest;

public interface SubscriptionService {

    List<PlanResponse> getPlans();

    SubscriptionCheckoutResponse createCheckout(
        UUID organizationId,
        String userEmail,
        String planCode
    );

    BillingCheckoutResponse createBillingOrder(
        UUID organizationId, String userEmail, String planCode
    );

    void verifyPayment(
        UUID organizationId, String userEmail, VerifyPaymentRequest request
    );

    void confirmOrderPayment(UUID billingOrderId, String paymentId);

    void verifyCheckout(
        UUID organizationId,
        UUID subscriptionId,
        String userEmail,
        VerifySubscriptionPaymentRequest request
    );

    void cancelSubscription(
        UUID organizationId,
        String userEmail
    );

    SubscriptionResponse getCurrentSubscription(
        UUID organizationId,
        String userEmail
    );

    NotificationPriority reserveNotificationQuota(
        UUID organizationId,
        NotificationChannel channel
    );
}
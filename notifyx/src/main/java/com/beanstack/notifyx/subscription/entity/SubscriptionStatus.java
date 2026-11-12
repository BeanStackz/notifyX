package com.beanstack.notifyx.subscription.entity;

public enum SubscriptionStatus {
    ACTIVE,
    PENDING_PAYMENT,
    AUTHENTICATED,
    PAST_DUE,
    HALTED,
    CANCELLED,
    EXPIRED,
    PAYMENT_CREATION_FAILED
}
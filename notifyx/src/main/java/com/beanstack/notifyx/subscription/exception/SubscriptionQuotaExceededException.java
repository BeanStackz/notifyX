package com.beanstack.notifyx.subscription.exception;

public class SubscriptionQuotaExceededException extends RuntimeException {
    public SubscriptionQuotaExceededException(String message) {
        super(message);
    }
}

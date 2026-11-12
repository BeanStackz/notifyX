package com.beanstack.notifyx.subscription.exception;

public class ActiveSubscriptionAlreadyExistsException extends RuntimeException {
    public ActiveSubscriptionAlreadyExistsException(String message){
        super(message);
    }
}

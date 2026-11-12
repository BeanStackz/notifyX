package com.beanstack.notifyx.subscription.exception;

public class NoActiveSubscriptionException extends RuntimeException{
    public NoActiveSubscriptionException(String message){
        super(message);
    }
}

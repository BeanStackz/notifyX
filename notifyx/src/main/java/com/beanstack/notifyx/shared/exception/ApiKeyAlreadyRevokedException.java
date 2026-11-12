package com.beanstack.notifyx.shared.exception;

public class ApiKeyAlreadyRevokedException extends RuntimeException {
    public ApiKeyAlreadyRevokedException(String message) {
        super(message);
    }
}

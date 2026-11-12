package com.beanstack.notifyx.shared.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProjectSlugAlreadyExistsException extends RuntimeException {
    public ProjectSlugAlreadyExistsException(String message) {
        super(message);
    }
    
}

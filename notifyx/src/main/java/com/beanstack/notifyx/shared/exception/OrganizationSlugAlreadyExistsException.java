package com.beanstack.notifyx.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OrganizationSlugAlreadyExistsException extends RuntimeException {
    public OrganizationSlugAlreadyExistsException(String slug) {
        super("Organization slug already exists: " + slug);
    }
}

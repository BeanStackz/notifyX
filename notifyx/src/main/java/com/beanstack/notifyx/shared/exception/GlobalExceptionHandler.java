package com.beanstack.notifyx.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.beanstack.notifyx.shared.response.ApiResponse;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler (EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler (OrganizationSlugAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrganizationSlugAlreadyExists(
                OrganizationSlugAlreadyExistsException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler (ProjectSlugAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectSlugAlreadyExists(
                ProjectSlugAlreadyExistsException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
 }

    
    @ExceptionHandler({
            BadCredentialsException.class,
            InvalidTokenException.class,
            RefreshTokenReuseException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationErrors(
            RuntimeException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler (ProjectArchivedException.class)
    public ResponseEntity<ApiResponse<Void>> handleProjectArchived(
            ProjectArchivedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }
    @ExceptionHandler(
        org.springframework.security.access.AccessDeniedException.class
    )
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException exception
    ) {
    return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("You do not have permission to perform this action"));
    }

    @ExceptionHandler (ApiKeyAlreadyRevokedException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiKeyAlreadyRevoked(
            ApiKeyAlreadyRevokedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(
            Exception ex
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }

}

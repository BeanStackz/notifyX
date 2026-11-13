package com.beanstack.notifyx.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beanstack.notifyx.shared.response.ApiResponse;
import com.beanstack.notifyx.subscription.exception.ActiveSubscriptionAlreadyExistsException;
import com.beanstack.notifyx.subscription.exception.NoActiveSubscriptionException;
import com.beanstack.notifyx.subscription.exception.RazorpayCheckoutCreationException;
import com.beanstack.notifyx.subscription.exception.SubscriptionQuotaExceededException;
import com.beanstack.notifyx.subscription.exception.UnsupportedNotificationChannelException;
import com.beanstack.notifyx.payment.exception.RazorpayPlanNotConfiguredException;

@RestControllerAdvice 
public class GlobalExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
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

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(
            MissingRequestHeaderException exception
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                        exception.getHeaderName() + " header is required"
                ));
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
        IllegalArgumentException exception
        ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(exception.getMessage()));
        }

    @ExceptionHandler(ActiveSubscriptionAlreadyExistsException.class)
        public ResponseEntity<ApiResponse<Void>> handleActiveSubscriptionExists(
        ActiveSubscriptionAlreadyExistsException exception
        ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(exception.getMessage()));
        }

        @ExceptionHandler(NoActiveSubscriptionException.class)
        public ResponseEntity<ApiResponse<Void>> handleNoActiveSubscription(
        NoActiveSubscriptionException exception
        ) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiResponse.error(exception.getMessage()));
        }

        @ExceptionHandler(SubscriptionQuotaExceededException.class)
        public ResponseEntity<ApiResponse<Void>> handleQuotaExceeded(
        SubscriptionQuotaExceededException exception
        ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(exception.getMessage()));
        }

        @ExceptionHandler(UnsupportedNotificationChannelException.class)
        public ResponseEntity<ApiResponse<Void>> handleUnsupportedChannel(
        UnsupportedNotificationChannelException exception
        ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(exception.getMessage()));
        }

    @ExceptionHandler(RazorpayCheckoutCreationException.class)
    public ResponseEntity<ApiResponse<Void>> handleRazorpayCheckoutFailure(
        RazorpayCheckoutCreationException exception
    ) {
        LOGGER.error("Razorpay checkout creation failed", exception);

        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(ApiResponse.error(
                "Unable to create the payment checkout. Verify the Razorpay "
                    + "plan configuration and try again."
            ));
    }

    @ExceptionHandler(RazorpayPlanNotConfiguredException.class)
    public ResponseEntity<ApiResponse<Void>> handleRazorpayPlanNotConfigured(
        RazorpayPlanNotConfiguredException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(
            Exception ex
    ) {
        LOGGER.error("Unhandled exception while processing request", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred"));
    }

}

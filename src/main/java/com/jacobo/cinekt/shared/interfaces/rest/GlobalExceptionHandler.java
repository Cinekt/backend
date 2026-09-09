package com.jacobo.cinekt.shared.interfaces.rest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jacobo.cinekt.shared.domain.model.exceptions.AuthenticationFailedException;
import com.jacobo.cinekt.shared.domain.model.exceptions.ResourceConflictException;
import com.jacobo.cinekt.shared.domain.model.exceptions.ResourceNotFoundException;
import com.jacobo.cinekt.shared.interfaces.rest.resources.ErrorResource;

/**
 * Translates exceptions raised inside the DispatcherServlet into a uniform JSON body.
 * <p>
 * Note that failures raised earlier in the Spring Security filter chain (an absent or
 * invalid bearer token, for instance) never reach this class: they are answered by
 * {@code UnauthorizedRequestHandlerEntryPoint} with Spring's default body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResource> handleResourceNotFound(ResourceNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResource> handleResourceConflict(ResourceConflictException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResource> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResource> handleValidation(MethodArgumentNotValidException exception) {
        var fieldErrors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), messageOf(error)));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResource> handleUnreadableBody(HttpMessageNotReadableException exception) {
        LOGGER.warn("Malformed request body: {}", exception.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", null);
    }

    /**
     * Method-level authorization only reaches this class when {@code @PreAuthorize} and friends
     * are used; URL-level rules are enforced in the filter chain and answered there. Handled
     * explicitly so that the catch-all below never downgrades a 403 into a 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResource> handleAccessDenied(AccessDeniedException exception) {
        return build(HttpStatus.FORBIDDEN, "Access denied", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResource> handleUnexpected(Exception exception) {
        // Spring's own MVC exceptions (unknown path, unsupported method, ...) already carry the
        // right status; honour it instead of reporting every one of them as a server error.
        if (exception instanceof ErrorResponse errorResponse) {
            var status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            LOGGER.warn("Request failed with {}: {}", status, exception.getMessage());
            return build(status, exception.getMessage(), null);
        }
        LOGGER.error("Unhandled exception", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private String messageOf(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "is invalid";
    }

    private ResponseEntity<ErrorResource> build(HttpStatus status, String message,
            Map<String, String> fieldErrors) {
        var body = new ErrorResource(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}

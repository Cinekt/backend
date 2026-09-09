package com.jacobo.cinekt.shared.domain.model.exceptions;

/**
 * Raised when a request conflicts with the current state of a resource. Mapped to HTTP 409.
 */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
}

package com.jacobo.cinekt.shared.domain.model.exceptions;

/**
 * Raised when credentials could not be verified. Mapped to HTTP 401.
 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}

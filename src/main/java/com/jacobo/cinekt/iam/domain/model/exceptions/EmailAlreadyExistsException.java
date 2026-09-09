package com.jacobo.cinekt.iam.domain.model.exceptions;

import com.jacobo.cinekt.shared.domain.model.exceptions.ResourceConflictException;

public class EmailAlreadyExistsException extends ResourceConflictException {
    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}

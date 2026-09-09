package com.jacobo.cinekt.iam.domain.model.exceptions;

import com.jacobo.cinekt.shared.domain.model.exceptions.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(Long userId) {
        super("User not found with id: " + userId);
    }
}

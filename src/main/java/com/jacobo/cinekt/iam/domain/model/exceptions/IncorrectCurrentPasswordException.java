package com.jacobo.cinekt.iam.domain.model.exceptions;

import com.jacobo.cinekt.shared.domain.model.exceptions.InvalidRequestException;

public class IncorrectCurrentPasswordException extends InvalidRequestException {
    public IncorrectCurrentPasswordException() {
        super("Current password is incorrect");
    }
}

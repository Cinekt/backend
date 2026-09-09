package com.jacobo.cinekt.iam.domain.model.exceptions;

import com.jacobo.cinekt.shared.domain.model.exceptions.AuthenticationFailedException;

/**
 * Raised when sign-in fails, whether the email is unknown or the password is wrong.
 * The message is fixed on purpose: telling the two cases apart would let a caller
 * enumerate registered accounts. The distinction is kept in the server logs only.
 */
public class InvalidCredentialsException extends AuthenticationFailedException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}

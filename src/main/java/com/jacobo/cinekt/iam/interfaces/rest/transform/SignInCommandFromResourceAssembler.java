package com.jacobo.cinekt.iam.interfaces.rest.transform;

import com.jacobo.cinekt.iam.domain.model.commands.SignInCommand;
import com.jacobo.cinekt.iam.interfaces.rest.resources.SignInResource;

public class SignInCommandFromResourceAssembler {
    public static SignInCommand toCommandFromResource(SignInResource signInResource) {
        return new SignInCommand(signInResource.email(), signInResource.password());
    }
}

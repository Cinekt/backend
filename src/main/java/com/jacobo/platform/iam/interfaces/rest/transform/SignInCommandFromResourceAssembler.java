package com.jacobo.platform.iam.interfaces.rest.transform;

import com.jacobo.platform.iam.domain.model.commands.SignInCommand;
import com.jacobo.platform.iam.interfaces.rest.resources.SignInResource;

public class SignInCommandFromResourceAssembler {
    public static SignInCommand toCommandFromResource(SignInResource signInResource) {
        return new SignInCommand(signInResource.username(), signInResource.password());
    }
}

package com.jacobo.platform.iam.interfaces.rest.transform;

import com.jacobo.platform.iam.domain.model.commands.SignUpCommand;
import com.jacobo.platform.iam.interfaces.rest.resources.SignUpResource;

public class SignUpCommandFromResourceAssembler {
    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        return new SignUpCommand(resource.username(), resource.password(), resource.roles());
    }
}

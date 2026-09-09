package com.jacobo.cinekt.iam.interfaces.rest.transform;

import com.jacobo.cinekt.iam.domain.model.aggregates.User;
import com.jacobo.cinekt.iam.domain.model.valueobjects.AuthenticationTokens;
import com.jacobo.cinekt.iam.interfaces.rest.resources.AuthenticatedUserResource;

public class AuthenticatedUserResourceFromEntityAssembler {
    public static AuthenticatedUserResource toResourceFromEntity(User user, AuthenticationTokens tokens) {
        return new AuthenticatedUserResource(user.getId(), user.getEmail(), tokens.accessToken(), tokens.refreshToken());
    }
}

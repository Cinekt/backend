package com.jacobo.cinekt.iam.domain.model.valueobjects;

import com.jacobo.cinekt.iam.domain.model.aggregates.User;

public record AuthenticatedUser(User user, AuthenticationTokens tokens) {
}

package com.jacobo.cinekt.iam.domain.services;

import java.util.Optional;

import com.jacobo.cinekt.iam.domain.model.aggregates.User;
import com.jacobo.cinekt.iam.domain.model.commands.*;
import com.jacobo.cinekt.iam.domain.model.valueobjects.AuthenticatedUser;

public interface UserCommandService {
    Optional<User> handle(SignUpCommand command);
    Optional<AuthenticatedUser> handle(SignInCommand command);
    Optional<String> handle(RefreshTokenCommand command);
    void handle(SignOutCommand command);
    void handle(SeedAdminCommand command);
    void handle(ChangeUserRoleCommand command);
}

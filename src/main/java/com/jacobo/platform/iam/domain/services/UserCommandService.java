package com.jacobo.platform.iam.domain.services;

import java.util.Optional;

import com.jacobo.platform.iam.domain.model.aggregates.User;
import com.jacobo.platform.iam.domain.model.commands.SignInCommand;
import com.jacobo.platform.iam.domain.model.commands.SignUpCommand;
import org.apache.commons.lang3.tuple.ImmutablePair;

public interface UserCommandService {
    Optional<User> handle(SignUpCommand command);

    Optional<ImmutablePair<User, String>> handle(SignInCommand command);
}

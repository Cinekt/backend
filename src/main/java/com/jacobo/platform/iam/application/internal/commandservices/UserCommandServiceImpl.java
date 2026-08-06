package com.jacobo.platform.iam.application.internal.commandservices;

import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import com.jacobo.platform.iam.application.internal.outboundedservices.hashing.HashingService;
import com.jacobo.platform.iam.application.internal.outboundedservices.tokens.TokenService;
import com.jacobo.platform.iam.domain.model.aggregates.User;
import com.jacobo.platform.iam.domain.model.commands.SignInCommand;
import com.jacobo.platform.iam.domain.model.commands.SignUpCommand;
import com.jacobo.platform.iam.domain.model.entities.Role;
import com.jacobo.platform.iam.domain.model.valueobjects.Roles;
import com.jacobo.platform.iam.domain.services.UserCommandService;
import com.jacobo.platform.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.jacobo.platform.iam.infrastructure.persistence.jpa.repositories.UserRepository;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;

    public UserCommandServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
            HashingService hashingService, TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
    }

    @Override
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByUsername(command.username()))
            throw new RuntimeException("Username already existe");
        var stringRoles = command.roles();
        var roles = new ArrayList<Role>();
        if (stringRoles == null || stringRoles.isEmpty()) {
            var storedRole = roleRepository.findByName(Roles.ROLE_USER);
            storedRole.ifPresent(roles::add);
        } else {
            stringRoles.forEach(role -> {
                var storedRole = roleRepository.findByName(Roles.valueOf(role));
                storedRole.ifPresent(roles::add);
            });
        }
        var user = new User(command.username(), hashingService.encode(command.password()), roles);
        userRepository.save(user);
        return userRepository.findByUsername(command.username());
    }

    @Override
    public Optional<ImmutablePair<User, String>> handle(SignInCommand command) {
        var user = userRepository.findByUsername(command.username());
        if (user.isEmpty())
            throw new RuntimeException("User not found");
        if (!hashingService.matches(command.password(), user.get().getPassword()))
            throw new RuntimeException("Invalid credentials");
        var token = tokenService.generateToken(user.get().getUsername());
        return Optional.of(new ImmutablePair<>(user.get(), token));
    }
}

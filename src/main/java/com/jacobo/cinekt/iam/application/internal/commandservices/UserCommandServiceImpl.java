package com.jacobo.cinekt.iam.application.internal.commandservices;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import com.jacobo.cinekt.iam.application.internal.outboundedservices.hashing.TokenHashingService;
import com.jacobo.cinekt.iam.domain.model.commands.*;
import com.jacobo.cinekt.iam.domain.model.exceptions.EmailAlreadyExistsException;
import com.jacobo.cinekt.iam.domain.model.exceptions.InvalidCredentialsException;
import com.jacobo.cinekt.iam.domain.model.exceptions.UserNotFoundException;
import com.jacobo.cinekt.iam.domain.model.entities.RefreshToken;
import com.jacobo.cinekt.iam.domain.model.valueobjects.AuthenticatedUser;
import com.jacobo.cinekt.iam.domain.model.valueobjects.AuthenticationTokens;
import com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jacobo.cinekt.iam.application.internal.outboundedservices.hashing.HashingService;
import com.jacobo.cinekt.iam.application.internal.outboundedservices.tokens.TokenService;
import com.jacobo.cinekt.iam.domain.model.aggregates.User;
import com.jacobo.cinekt.iam.domain.model.entities.Role;
import com.jacobo.cinekt.iam.domain.model.valueobjects.Roles;
import com.jacobo.cinekt.iam.domain.services.UserCommandService;
import com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserCommandServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final HashingService hashingService;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashingService tokenHashingService;

    public UserCommandServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
            HashingService hashingService, TokenService tokenService, RefreshTokenRepository refreshTokenRepository, TokenHashingService tokenHashingService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.hashingService = hashingService;
        this.tokenService = tokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashingService = tokenHashingService;
    }

    @Override
    @Transactional
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByEmail(command.email()))
            throw new EmailAlreadyExistsException(command.email());
        var roles = new ArrayList<Role>();
        var storedRole = roleRepository.findByName(Roles.ROLE_USER);
        storedRole.ifPresent(roles::add);
        var user = new User(command.email(), hashingService.encode(command.password()), roles);
        userRepository.save(user);
        return userRepository.findByEmail(command.email());
    }

    @Override
    @Transactional
    public Optional<AuthenticatedUser> handle(SignInCommand command) {
        var user = userRepository.findByEmail(command.email());
        if (user.isEmpty()) {
            LOGGER.warn("Sign-in rejected: no account registered with email {}", command.email());
            throw new InvalidCredentialsException();
        }
        if (!hashingService.matches(command.password(), user.get().getPassword())) {
            LOGGER.warn("Sign-in rejected: wrong password for email {}", command.email());
            throw new InvalidCredentialsException();
        }
        var accessToken = tokenService.generateAccessToken(user.get().getEmail());
        var refreshToken = tokenService.generateRefreshToken(user.get().getEmail());
        var refreshTokenHash = tokenHashingService.hash(refreshToken);
        var refreshTokenEntity = new RefreshToken(refreshTokenHash, user.get(), extractExpiration(refreshToken));
        refreshTokenRepository.save(refreshTokenEntity);
        var tokens = new AuthenticationTokens(accessToken, refreshToken);
        return Optional.of(new AuthenticatedUser(user.get(),tokens));
    }

    @Override
    @Transactional
    public Optional<String> handle(RefreshTokenCommand command) {
        var refreshTokenHash = tokenHashingService.hash(command.refreshToken());
        var refreshTokenEntity = refreshTokenRepository.findByTokenHash(refreshTokenHash);
        if (refreshTokenEntity.isEmpty()) {
            return Optional.empty();
        }
        var storedRefreshToken = refreshTokenEntity.get();
        if (!storedRefreshToken.isValid()) {
            return Optional.empty();
        }
        if (!tokenService.validateRefreshToken(command.refreshToken())) {
            return Optional.empty();
        }
        String email = tokenService.getEmailFromToken(command.refreshToken());
        if (!storedRefreshToken.getUser()
                .getEmail()
                .equals(email)) {
            return Optional.empty();
        }
        String newAccessToken = tokenService.generateAccessToken(email);
        return Optional.of(newAccessToken);
    }

    private Instant extractExpiration(String token){
        return tokenService.getExpirationFromToken(token).toInstant();
    }

    @Override
    @Transactional
    public void handle(SignOutCommand command){
        var refreshTokenHash = tokenHashingService.hash(command.refreshToken());
        refreshTokenRepository.findByTokenHash(refreshTokenHash)
                .filter(RefreshToken::isValid)
                .ifPresent(token ->{
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }

    @Override
    @Transactional
    public void handle(SeedAdminCommand command){
        if(userRepository.existsByEmail(command.email())){
            return;
        }
        var adminRole = roleRepository.findByName(Roles.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_ADMIN is missing from the roles catalog; role seeding did not run"));

        var admin = new User(
                command.email(),
                hashingService.encode(command.password()),
                new ArrayList<>()
        );
        admin.addRole(adminRole);
        userRepository.save(admin);
    }

    @Override
    @Transactional
    public void handle(ChangeUserRoleCommand command) {
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));
        var role = roleRepository.findByName(command.role())
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + command.role() + " is missing from the roles catalog; role seeding did not run"));
        user.getRoles().clear();
        user.addRole(role);
        userRepository.save(user);
    }


}

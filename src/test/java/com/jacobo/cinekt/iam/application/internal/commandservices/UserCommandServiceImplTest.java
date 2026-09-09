package com.jacobo.cinekt.iam.application.internal.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jacobo.cinekt.iam.application.internal.outboundedservices.hashing.HashingService;
import com.jacobo.cinekt.iam.application.internal.outboundedservices.hashing.TokenHashingService;
import com.jacobo.cinekt.iam.application.internal.outboundedservices.tokens.TokenService;
import com.jacobo.cinekt.iam.domain.model.aggregates.User;
import com.jacobo.cinekt.iam.domain.model.commands.ChangeUserRoleCommand;
import com.jacobo.cinekt.iam.domain.model.commands.RefreshTokenCommand;
import com.jacobo.cinekt.iam.domain.model.commands.SeedAdminCommand;
import com.jacobo.cinekt.iam.domain.model.commands.SignInCommand;
import com.jacobo.cinekt.iam.domain.model.commands.SignOutCommand;
import com.jacobo.cinekt.iam.domain.model.commands.SignUpCommand;
import com.jacobo.cinekt.iam.domain.model.entities.RefreshToken;
import com.jacobo.cinekt.iam.domain.model.entities.Role;
import com.jacobo.cinekt.iam.domain.model.exceptions.EmailAlreadyExistsException;
import com.jacobo.cinekt.iam.domain.model.exceptions.InvalidCredentialsException;
import com.jacobo.cinekt.iam.domain.model.exceptions.UserNotFoundException;
import com.jacobo.cinekt.iam.domain.model.valueobjects.Roles;
import com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories.RefreshTokenRepository;
import com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    private static final String EMAIL = "user@cinekt.local";
    private static final String RAW_PASSWORD = "raw-password";
    private static final String HASHED_PASSWORD = "hashed-password";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private HashingService hashingService;
    @Mock
    private TokenService tokenService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private TokenHashingService tokenHashingService;

    private UserCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserCommandServiceImpl(userRepository, roleRepository, hashingService, tokenService,
                refreshTokenRepository, tokenHashingService);
    }

    @Nested
    class SignUp {

        @Test
        void rejectsAnEmailThatIsAlreadyRegistered() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> service.handle(new SignUpCommand(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void hashesThePasswordAndAssignsTheDefaultRoleForANewEmail() {
            var userRole = new Role(Roles.ROLE_USER);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(roleRepository.findByName(Roles.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(hashingService.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            var persistedUser = new User(EMAIL, HASHED_PASSWORD, List.of(userRole));
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persistedUser));

            var result = service.handle(new SignUpCommand(EMAIL, RAW_PASSWORD));

            var savedUserCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(savedUserCaptor.capture());
            var savedUser = savedUserCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
            assertThat(savedUser.getPassword()).isEqualTo(HASHED_PASSWORD);
            assertThat(savedUser.getRoles()).containsExactly(userRole);
            assertThat(result).contains(persistedUser);
        }
    }

    @Nested
    class SignIn {

        @Test
        void rejectsAnUnknownEmail() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.handle(new SignInCommand(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        void rejectsAWrongPassword() {
            var user = new User(EMAIL, HASHED_PASSWORD);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(hashingService.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> service.handle(new SignInCommand(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        void issuesTokensAndPersistsTheHashedRefreshTokenOnSuccess() {
            var user = new User(EMAIL, HASHED_PASSWORD);
            var expiration = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(hashingService.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(tokenService.generateAccessToken(EMAIL)).thenReturn("access-token");
            when(tokenService.generateRefreshToken(EMAIL)).thenReturn("refresh-token");
            when(tokenHashingService.hash("refresh-token")).thenReturn("hashed-refresh-token");
            when(tokenService.getExpirationFromToken("refresh-token")).thenReturn(expiration);

            var result = service.handle(new SignInCommand(EMAIL, RAW_PASSWORD));

            assertThat(result).isPresent();
            assertThat(result.get().user()).isEqualTo(user);
            assertThat(result.get().tokens().accessToken()).isEqualTo("access-token");
            assertThat(result.get().tokens().refreshToken()).isEqualTo("refresh-token");

            var refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
            var savedRefreshToken = refreshTokenCaptor.getValue();
            assertThat(savedRefreshToken.getTokenHash()).isEqualTo("hashed-refresh-token");
            assertThat(savedRefreshToken.getUser()).isEqualTo(user);
            assertThat(savedRefreshToken.getExpiresAt()).isEqualTo(expiration.toInstant());
        }
    }

    @Nested
    class HandleRefreshTokenCommand {

        private final User user = new User(EMAIL, HASHED_PASSWORD);

        @BeforeEach
        void stubHashing() {
            when(tokenHashingService.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        }

        @Test
        void returnsEmptyWhenNoStoredTokenMatchesTheHash() {
            when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.empty());

            var result = service.handle(new RefreshTokenCommand("raw-refresh-token"));

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyWhenTheStoredTokenIsExpiredOrRevoked() {
            var expiredToken = new RefreshToken("hashed-refresh-token", user, Instant.now().minus(1, ChronoUnit.DAYS));
            when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(expiredToken));

            var result = service.handle(new RefreshTokenCommand("raw-refresh-token"));

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyWhenTheJwtItselfFailsValidation() {
            var validStoredToken = new RefreshToken("hashed-refresh-token", user, Instant.now().plus(1, ChronoUnit.DAYS));
            when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(validStoredToken));
            when(tokenService.validateRefreshToken("raw-refresh-token")).thenReturn(false);

            var result = service.handle(new RefreshTokenCommand("raw-refresh-token"));

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyWhenTheJwtEmailDoesNotMatchTheStoredTokensOwner() {
            var validStoredToken = new RefreshToken("hashed-refresh-token", user, Instant.now().plus(1, ChronoUnit.DAYS));
            when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(validStoredToken));
            when(tokenService.validateRefreshToken("raw-refresh-token")).thenReturn(true);
            when(tokenService.getEmailFromToken("raw-refresh-token")).thenReturn("someone-else@cinekt.local");

            var result = service.handle(new RefreshTokenCommand("raw-refresh-token"));

            assertThat(result).isEmpty();
        }

        @Test
        void returnsANewAccessTokenWhenEverythingIsValid() {
            var validStoredToken = new RefreshToken("hashed-refresh-token", user, Instant.now().plus(1, ChronoUnit.DAYS));
            when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(validStoredToken));
            when(tokenService.validateRefreshToken("raw-refresh-token")).thenReturn(true);
            when(tokenService.getEmailFromToken("raw-refresh-token")).thenReturn(EMAIL);
            when(tokenService.generateAccessToken(EMAIL)).thenReturn("new-access-token");

            var result = service.handle(new RefreshTokenCommand("raw-refresh-token"));

            assertThat(result).contains("new-access-token");
        }
    }

    @Nested
    class SignOut {

        private final User user = new User(EMAIL, HASHED_PASSWORD);

        @BeforeEach
        void stubHashing() {
            when(tokenHashingService.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        }

        @Test
        void revokesAndPersistsAValidStoredToken() {
            var storedToken = new RefreshToken("hashed-refresh-token", user, Instant.now().plus(1, ChronoUnit.DAYS));
            when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(storedToken));

            service.handle(new SignOutCommand("raw-refresh-token"));

            assertThat(storedToken.isValid()).isFalse();
            verify(refreshTokenRepository).save(storedToken);
        }

        @Test
        void doesNothingWhenNoStoredTokenMatchesTheHash() {
            when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.empty());

            service.handle(new SignOutCommand("raw-refresh-token"));

            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        void doesNothingWhenTheStoredTokenIsAlreadyInvalid() {
            var alreadyRevokedToken = new RefreshToken("hashed-refresh-token", user, Instant.now().plus(1, ChronoUnit.DAYS));
            alreadyRevokedToken.revoke();
            when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(alreadyRevokedToken));

            service.handle(new SignOutCommand("raw-refresh-token"));

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    class SeedAdmin {

        @Test
        void doesNothingWhenAnAdminAlreadyExistsForThatEmail() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            service.handle(new SeedAdminCommand(EMAIL, RAW_PASSWORD));

            verify(roleRepository, never()).findByName(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        void createsAnAdminWithTheAdminRoleWhenNoneExistsYet() {
            var adminRole = new Role(Roles.ROLE_ADMIN);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(roleRepository.findByName(Roles.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
            when(hashingService.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);

            service.handle(new SeedAdminCommand(EMAIL, RAW_PASSWORD));

            var savedUserCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(savedUserCaptor.capture());
            var savedAdmin = savedUserCaptor.getValue();
            assertThat(savedAdmin.getEmail()).isEqualTo(EMAIL);
            assertThat(savedAdmin.getPassword()).isEqualTo(HASHED_PASSWORD);
            assertThat(savedAdmin.getRoles()).containsExactly(adminRole);
        }

        @Test
        void failsFastWhenTheAdminRoleIsMissingFromTheCatalog() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(roleRepository.findByName(Roles.ROLE_ADMIN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.handle(new SeedAdminCommand(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(IllegalStateException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class ChangeUserRole {

        private static final Long USER_ID = 1L;

        @Test
        void throwsWhenTheUserDoesNotExist() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.handle(new ChangeUserRoleCommand(USER_ID, Roles.ROLE_ADMIN)))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void throwsWhenTheTargetRoleIsMissingFromTheCatalog() {
            var user = new User(EMAIL, HASHED_PASSWORD);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(Roles.ROLE_ADMIN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.handle(new ChangeUserRoleCommand(USER_ID, Roles.ROLE_ADMIN)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void replacesTheUsersExistingRolesWithTheRequestedOne() {
            var currentRole = new Role(Roles.ROLE_USER);
            var newRole = new Role(Roles.ROLE_ADMIN);
            var user = new User(EMAIL, HASHED_PASSWORD, new ArrayList<>(List.of(currentRole)));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findByName(Roles.ROLE_ADMIN)).thenReturn(Optional.of(newRole));

            service.handle(new ChangeUserRoleCommand(USER_ID, Roles.ROLE_ADMIN));

            assertThat(user.getRoles()).containsExactly(newRole);
            verify(userRepository).save(user);
        }
    }
}

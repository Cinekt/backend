package com.jacobo.cinekt.iam.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.jacobo.cinekt.iam.domain.model.aggregates.User;

class RefreshTokenTest {

    private final User user = new User("user@cinekt.local", "hashed-password");

    @Nested
    class IsExpired {

        @Test
        void returnsFalseWhenExpirationIsInTheFuture() {
            var token = new RefreshToken("hash", user, Instant.now().plus(1, ChronoUnit.DAYS));

            assertThat(token.isExpired()).isFalse();
        }

        @Test
        void returnsTrueWhenExpirationIsInThePast() {
            var token = new RefreshToken("hash", user, Instant.now().minus(1, ChronoUnit.DAYS));

            assertThat(token.isExpired()).isTrue();
        }

        @Test
        void returnsTrueWhenExpirationIsExactlyNow() {
            var now = Instant.now();
            var token = new RefreshToken("hash", user, now);

            assertThat(token.isExpired()).isTrue();
        }
    }

    @Nested
    class IsValid {

        @Test
        void returnsTrueWhenNotExpiredAndNotRevoked() {
            var token = new RefreshToken("hash", user, Instant.now().plus(1, ChronoUnit.DAYS));

            assertThat(token.isValid()).isTrue();
        }

        @Test
        void returnsFalseWhenExpiredEvenIfNotRevoked() {
            var token = new RefreshToken("hash", user, Instant.now().minus(1, ChronoUnit.DAYS));

            assertThat(token.isValid()).isFalse();
        }

        @Test
        void returnsFalseWhenRevokedEvenIfNotExpired() {
            var token = new RefreshToken("hash", user, Instant.now().plus(1, ChronoUnit.DAYS));
            token.revoke();

            assertThat(token.isValid()).isFalse();
        }

        @Test
        void returnsFalseWhenBothExpiredAndRevoked() {
            var token = new RefreshToken("hash", user, Instant.now().minus(1, ChronoUnit.DAYS));
            token.revoke();

            assertThat(token.isValid()).isFalse();
        }
    }

    @Nested
    class Revoke {

        @Test
        void marksAPreviouslyValidTokenAsInvalid() {
            var token = new RefreshToken("hash", user, Instant.now().plus(1, ChronoUnit.DAYS));
            assertThat(token.isValid()).isTrue();

            token.revoke();

            assertThat(token.isValid()).isFalse();
        }
    }
}

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

    @Nested
    class Rotate {

        @Test
        void revokesTheOldTokenAndReturnsAReplacementInTheSameFamily() {
            // Arrange
            var oldToken = new RefreshToken("old-hash", user, Instant.now().plus(1, ChronoUnit.DAYS));
            var newExpiration = Instant.now().plus(7, ChronoUnit.DAYS);

            // Act
            var newToken = oldToken.rotate("new-hash", newExpiration);

            // Assert
            assertThat(oldToken.isRevoked()).isTrue();
            assertThat(newToken.isValid()).isTrue();
            assertThat(newToken.getTokenHash()).isEqualTo("new-hash");
            assertThat(newToken.getExpiresAt()).isEqualTo(newExpiration);
            assertThat(newToken.getFamilyId()).isEqualTo(oldToken.getFamilyId());
            assertThat(newToken.getUser()).isSameAs(user);
        }

        @Test
        void keepsTheWholeChainInTheSameFamilyAcrossSeveralRotations() {
            // Arrange
            var first = new RefreshToken("hash-1", user, Instant.now().plus(1, ChronoUnit.DAYS));

            // Act
            var third = first.rotate("hash-2", Instant.now().plus(1, ChronoUnit.DAYS))
                    .rotate("hash-3", Instant.now().plus(1, ChronoUnit.DAYS));

            // Assert
            assertThat(third.getFamilyId()).isEqualTo(first.getFamilyId());
        }
    }

    @Nested
    class FamilyId {

        @Test
        void startsANewFamilyForEachFreshlyConstructedToken() {
            // Arrange & Act
            var oneSession = new RefreshToken("hash-a", user, Instant.now().plus(1, ChronoUnit.DAYS));
            var anotherSession = new RefreshToken("hash-b", user, Instant.now().plus(1, ChronoUnit.DAYS));

            // Assert
            assertThat(oneSession.getFamilyId()).isNotBlank();
            assertThat(oneSession.getFamilyId()).isNotEqualTo(anotherSession.getFamilyId());
        }
    }
}

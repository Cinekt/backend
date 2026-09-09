package com.jacobo.cinekt.iam.infrastructure.tokens.jwt.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class TokenServiceImplTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long";
    private static final String EMAIL = "user@cinekt.local";

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokenService, "secret", SECRET);
        ReflectionTestUtils.setField(tokenService, "accessTokenExpirationMinutes", 2);
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpirationDays", 7);
    }

    @Nested
    class GenerateAccessToken {

        @Test
        void encodesTheGivenEmailAsTheSubject() {
            var token = tokenService.generateAccessToken(EMAIL);

            assertThat(tokenService.getEmailFromToken(token)).isEqualTo(EMAIL);
        }

        @Test
        void isRecognizedAsAnAccessTokenAndNotARefreshToken() {
            var token = tokenService.generateAccessToken(EMAIL);

            assertThat(tokenService.validateAccessToken(token)).isTrue();
            assertThat(tokenService.validateRefreshToken(token)).isFalse();
        }

        @Test
        void expiresRoughlyAfterTheConfiguredMinutes() {
            var before = new Date();
            var token = tokenService.generateAccessToken(EMAIL);

            var expiration = tokenService.getExpirationFromToken(token);

            long expectedExpirationMillis = before.getTime() + 2 * 60_000L;
            assertThat(expiration.getTime()).isCloseTo(expectedExpirationMillis, offset(5_000L));
        }
    }

    @Nested
    class GenerateRefreshToken {

        @Test
        void encodesTheGivenEmailAsTheSubject() {
            var token = tokenService.generateRefreshToken(EMAIL);

            assertThat(tokenService.getEmailFromToken(token)).isEqualTo(EMAIL);
        }

        @Test
        void isRecognizedAsARefreshTokenAndNotAnAccessToken() {
            var token = tokenService.generateRefreshToken(EMAIL);

            assertThat(tokenService.validateRefreshToken(token)).isTrue();
            assertThat(tokenService.validateAccessToken(token)).isFalse();
        }

        @Test
        void expiresRoughlyAfterTheConfiguredDays() {
            var before = new Date();
            var token = tokenService.generateRefreshToken(EMAIL);

            var expiration = tokenService.getExpirationFromToken(token);

            long expectedExpirationMillis = before.getTime() + 7 * 24 * 60 * 60_000L;
            assertThat(expiration.getTime()).isCloseTo(expectedExpirationMillis, offset(5_000L));
        }
    }

    @Nested
    class TokenValidation {

        @Test
        void rejectsAnExpiredToken() {
            var expiredToken = tokenSignedWith(SECRET, "REFRESH", new Date(System.currentTimeMillis() - 10_000),
                    new Date(System.currentTimeMillis() - 5_000));

            assertThat(tokenService.validateRefreshToken(expiredToken)).isFalse();
        }

        @Test
        void rejectsATokenSignedWithADifferentSecret() {
            var otherSecret = "a-completely-different-secret-key-of-32-bytes+";
            var tamperedToken = tokenSignedWith(otherSecret, "ACCESS", new Date(),
                    new Date(System.currentTimeMillis() + 60_000));

            assertThat(tokenService.validateAccessToken(tamperedToken)).isFalse();
        }

        @Test
        void rejectsAMalformedToken() {
            assertThat(tokenService.validateAccessToken("not-a-jwt")).isFalse();
        }
    }

    @Nested
    class GetBearerTokenFrom {

        @Test
        void extractsTheTokenWhenTheHeaderHasTheBearerPrefix() {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer abc.def.ghi");

            assertThat(tokenService.getBearerTokenFrom(request)).isEqualTo("abc.def.ghi");
        }

        @Test
        void returnsNullWhenTheHeaderIsMissing() {
            var request = new MockHttpServletRequest();

            assertThat(tokenService.getBearerTokenFrom(request)).isNull();
        }

        @Test
        void returnsNullWhenTheHeaderDoesNotHaveTheBearerPrefix() {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "abc.def.ghi");

            assertThat(tokenService.getBearerTokenFrom(request)).isNull();
        }
    }

    private static String tokenSignedWith(String secret, String tokenType, Date issuedAt, Date expiration) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(TokenServiceImplTest.EMAIL)
                .claim("type", tokenType)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }
}

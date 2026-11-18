package com.jacobo.cinekt.iam.interfaces.rest.cookies;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/authentication";

    private final boolean secure;
    private final String sameSite;
    private final int expirationDays;

    public RefreshTokenCookieFactory(
            @Value("${authorization.refresh-token.cookie.secure}") boolean secure,
            @Value("${authorization.refresh-token.cookie.same-site}") String sameSite,
            @Value("${authorization.jwt.refresh-token.expiration.days}") int expirationDays) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.expirationDays = expirationDays;
    }

    public ResponseCookie build(String refreshToken) {
        return baseBuilder(refreshToken)
                .maxAge(Duration.ofDays(expirationDays))
                .build();
    }

    public ResponseCookie expired() {
        return baseBuilder("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH);
    }
}

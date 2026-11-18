package com.jacobo.cinekt.iam.interfaces.rest.cookies;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class RefreshTokenCookieFactoryTest {

    @Test
    void buildsAnHttpOnlyCookieScopedToTheAuthenticationPath() {
        // Arrange
        var factory = new RefreshTokenCookieFactory(false, "Lax", 7);

        // Act
        var cookie = factory.build("raw-refresh-token");

        // Assert
        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("raw-refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/authentication");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void appliesTheConfiguredSecureAndSameSiteAttributes() {
        // Arrange
        var productionFactory = new RefreshTokenCookieFactory(true, "None", 30);

        // Act
        var cookie = productionFactory.build("raw-refresh-token");

        // Assert
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("None");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void buildsAnImmediatelyExpiringCookieForSignOut() {
        // Arrange
        var factory = new RefreshTokenCookieFactory(false, "Lax", 7);

        // Act
        var cookie = factory.expired();

        // Assert
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/authentication");
    }
}

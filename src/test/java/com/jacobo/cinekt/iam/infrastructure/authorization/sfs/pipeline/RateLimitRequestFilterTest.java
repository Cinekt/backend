package com.jacobo.cinekt.iam.infrastructure.authorization.sfs.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.github.bucket4j.TimeMeter;

class RateLimitRequestFilterTest {

    private static final int CAPACITY = 2;

    private final RateLimitRequestFilter filter =
            new RateLimitRequestFilter(CAPACITY, Duration.ofMinutes(1), 100);

    private MockHttpServletRequest signInRequestFrom(String clientAddress) {
        var request = new MockHttpServletRequest("POST", "/api/v1/authentication/sign-in");
        request.setRemoteAddr(clientAddress);
        return request;
    }

    @Test
    void letsRequestsThroughWhileTheBudgetHasTokens() throws Exception {
        // Arrange
        var request = signInRequestFrom("1.1.1.1");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void rejectsWithTooManyRequestsOnceTheBudgetIsExhausted() throws Exception {
        // Arrange
        for (var attempt = 0; attempt < CAPACITY; attempt++) {
            filter.doFilter(signInRequestFrom("1.1.1.1"), new MockHttpServletResponse(), new MockFilterChain());
        }
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        // Act
        filter.doFilter(signInRequestFrom("1.1.1.1"), response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void keepsASeparateBudgetPerClientAddress() throws Exception {
        // Arrange
        for (var attempt = 0; attempt <= CAPACITY; attempt++) {
            filter.doFilter(signInRequestFrom("1.1.1.1"), new MockHttpServletResponse(), new MockFilterChain());
        }
        var request = signInRequestFrom("2.2.2.2");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void keepsRejectingUntilTheWholeWindowHasElapsed() throws Exception {
        // Arrange
        var clock = new FakeClock();
        var filterWithFakeClock = new RateLimitRequestFilter(CAPACITY, Duration.ofMinutes(1), 100, clock);
        for (var attempt = 0; attempt <= CAPACITY; attempt++) {
            filterWithFakeClock.doFilter(signInRequestFrom("1.1.1.1"), new MockHttpServletResponse(),
                    new MockFilterChain());
        }
        var halfwayResponse = new MockHttpServletResponse();
        var afterTheWindowResponse = new MockHttpServletResponse();

        // Act
        clock.advance(Duration.ofSeconds(30));
        filterWithFakeClock.doFilter(signInRequestFrom("1.1.1.1"), halfwayResponse, new MockFilterChain());
        clock.advance(Duration.ofSeconds(31));
        filterWithFakeClock.doFilter(signInRequestFrom("1.1.1.1"), afterTheWindowResponse, new MockFilterChain());

        // Assert
        assertThat(halfwayResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(afterTheWindowResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void ignoresEndpointsThatAreNotRateLimited() throws Exception {
        // Arrange
        for (var attempt = 0; attempt <= CAPACITY; attempt++) {
            filter.doFilter(signInRequestFrom("1.1.1.1"), new MockHttpServletResponse(), new MockFilterChain());
        }
        var request = new MockHttpServletRequest("POST", "/api/v1/authentication/sign-out");
        request.setRemoteAddr("1.1.1.1");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private static class FakeClock implements TimeMeter {

        private long nanos;

        void advance(Duration elapsed) {
            nanos += elapsed.toNanos();
        }

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    }
}

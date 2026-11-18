package com.jacobo.cinekt.iam.infrastructure.authorization.sfs.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;

class CsrfHeaderRequestFilterTest {

    private final CsrfHeaderRequestFilter filter = new CsrfHeaderRequestFilter();

    @Test
    void rejectsARefreshRequestWithoutTheCustomHeader() throws Exception {
        // Arrange
        var request = new MockHttpServletRequest("POST", "/api/v1/authentication/refresh");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void letsARefreshRequestWithTheCustomHeaderThrough() throws Exception {
        // Arrange
        var request = new MockHttpServletRequest("POST", "/api/v1/authentication/refresh");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void rejectsASignOutRequestWithoutTheCustomHeader() throws Exception {
        // Arrange
        var request = new MockHttpServletRequest("POST", "/api/v1/authentication/sign-out");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void ignoresEndpointsOtherThanRefreshAndSignOut() throws Exception {
        // Arrange
        var request = new MockHttpServletRequest("POST", "/api/v1/authentication/sign-in");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        assertThat(chain.getRequest()).isSameAs(request);
    }
}

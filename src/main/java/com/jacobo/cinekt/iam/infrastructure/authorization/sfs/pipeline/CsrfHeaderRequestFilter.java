package com.jacobo.cinekt.iam.infrastructure.authorization.sfs.pipeline;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CsrfHeaderRequestFilter extends OncePerRequestFilter {

    public static final String REQUIRED_HEADER_NAME = "X-Requested-With";
    public static final String REQUIRED_HEADER_VALUE = "XMLHttpRequest";

    private static final Logger LOGGER = LoggerFactory.getLogger(CsrfHeaderRequestFilter.class);
    private static final Set<String> COOKIE_AUTHENTICATED_PATHS = Set.of(
            "/api/v1/authentication/refresh",
            "/api/v1/authentication/sign-out");

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !COOKIE_AUTHENTICATED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!REQUIRED_HEADER_VALUE.equals(request.getHeader(REQUIRED_HEADER_NAME))) {
            LOGGER.warn("Rejected {} without the {} header", request.getRequestURI(), REQUIRED_HEADER_NAME);
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Missing " + REQUIRED_HEADER_NAME + " header");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

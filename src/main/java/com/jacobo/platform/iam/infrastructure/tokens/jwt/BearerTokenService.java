package com.jacobo.platform.iam.infrastructure.tokens.jwt;

import org.springframework.security.core.Authentication;

import com.jacobo.platform.iam.application.internal.outboundedservices.tokens.TokenService;

import jakarta.servlet.http.HttpServletRequest;

public interface BearerTokenService extends TokenService {
    String getBearerTokenFrom(HttpServletRequest request);

    String generateToken(Authentication authentication);
}

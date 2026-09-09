package com.jacobo.cinekt.iam.infrastructure.tokens.jwt;

import com.jacobo.cinekt.iam.application.internal.outboundedservices.tokens.TokenService;

import jakarta.servlet.http.HttpServletRequest;

public interface BearerTokenService extends TokenService {
    String getBearerTokenFrom(HttpServletRequest request);
}

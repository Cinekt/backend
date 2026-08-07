package com.jacobo.cinekt.iam.application.internal.outboundedservices.tokens;

public interface TokenService {
    String generateToken(String username);

    String getUsernameFromToken(String token);

    boolean validateToken(String token);
}

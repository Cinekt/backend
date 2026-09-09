package com.jacobo.cinekt.iam.application.internal.outboundedservices.tokens;

import java.util.Date;

public interface TokenService {

    String generateAccessToken(String email);

    String generateRefreshToken(String email);

    String getEmailFromToken(String token);

    boolean validateAccessToken(String token);

    boolean validateRefreshToken(String token);

    Date getExpirationFromToken(String token);
}

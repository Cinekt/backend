package com.jacobo.cinekt.iam.infrastructure.tokens.jwt.services;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.jacobo.cinekt.iam.infrastructure.tokens.jwt.BearerTokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class TokenServiceImpl implements BearerTokenService {

    private static final String AUTHORIZATION_PARAMETER_NAME = "Authorization";
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static final int TOKEN_BEGIN_INDEX = 7;

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final Logger LOGGER = LoggerFactory.getLogger(TokenServiceImpl.class);


    @Value("${authorization.jwt.secret}")
    private String secret;

    @Value("${authorization.jwt.access-token.expiration.minutes}")
    private int accessTokenExpirationMinutes;

    @Value("${authorization.jwt.refresh-token.expiration.days}")
    private int refreshTokenExpirationDays;


    @Override
    public String getBearerTokenFrom(HttpServletRequest request) {
        String parameter = getAuthorizationParameterFrom(request);
        if (isTokenPresentIn(parameter) && isBearerTokenIn(parameter))
            return extractTokenFrom(parameter);
        return null;
    }

    @Override
    public String generateAccessToken(String username) {
        var issuedAt = new Date();
        var expiration = DateUtils.addMinutes(issuedAt,accessTokenExpirationMinutes);
        return buildToken(username,ACCESS_TOKEN_TYPE,issuedAt,expiration);
    }

    @Override
    public String generateRefreshToken(String username) {
        var issuedAt = new Date();
        var expiration = DateUtils.addDays(issuedAt,refreshTokenExpirationDays);
        return buildToken(username,REFRESH_TOKEN_TYPE,issuedAt,expiration);
    }

    @Override
    public String getEmailFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean validateAccessToken(String token){
        return validateTokenType(token, ACCESS_TOKEN_TYPE);
    }

    @Override
    public boolean validateRefreshToken(String token){
        return validateTokenType(token, REFRESH_TOKEN_TYPE);
    }

    private boolean validateTokenType(String token, String expectedType) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return expectedType.equals(tokenType);
        } catch (SignatureException e) {
            LOGGER.error("Invalid JSON Web Token signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            LOGGER.error("Invalid JSON Web Token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            LOGGER.error("Invalid JSON Web Token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            LOGGER.error("Unsupported JSON Web Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid JSON Web Token: {}", e.getMessage());
        }
        return false;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String buildToken(String username, String tokenType, Date issuedAt, Date expiration) {
        SecretKey key = getSigningKey();
        return Jwts.builder()
                .subject(username)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    @Override
    public Date getExpirationFromToken(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenPresentIn(String authorizationHeaderParameter) {
        return StringUtils.hasText(authorizationHeaderParameter);
    }

    private boolean isBearerTokenIn(String authorizationHeaderParameter) {
        return authorizationHeaderParameter.startsWith(BEARER_TOKEN_PREFIX);
    }

    private String extractTokenFrom(String authorizationHeaderParameter) {
        return authorizationHeaderParameter.substring(TOKEN_BEGIN_INDEX);
    }

    private String getAuthorizationParameterFrom(HttpServletRequest request) {
        return request.getHeader(AUTHORIZATION_PARAMETER_NAME);
    }

}

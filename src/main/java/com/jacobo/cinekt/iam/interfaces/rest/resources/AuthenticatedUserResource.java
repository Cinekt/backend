package com.jacobo.cinekt.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(Long id, String email, String accessToken, String refreshToken) {
}

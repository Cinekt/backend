package com.jacobo.cinekt.iam.interfaces;

import com.jacobo.cinekt.iam.domain.model.commands.RefreshTokenCommand;
import com.jacobo.cinekt.iam.domain.model.commands.SignOutCommand;
import com.jacobo.cinekt.iam.interfaces.rest.cookies.RefreshTokenCookieFactory;
import com.jacobo.cinekt.iam.interfaces.rest.resources.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacobo.cinekt.iam.domain.services.UserCommandService;
import com.jacobo.cinekt.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.jacobo.cinekt.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.jacobo.cinekt.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.jacobo.cinekt.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Sign-up, sign-in and session management endpoints")
public class AuthenticationController {

    private static final String CSRF_HEADER_DESCRIPTION =
            "Required. Proves the request was issued by the frontend and not by a cross-site form.";

    private final UserCommandService userCommandService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public AuthenticationController(UserCommandService userCommandService,
            RefreshTokenCookieFactory refreshTokenCookieFactory) {
        this.userCommandService = userCommandService;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthenticatedUserResource> signIn(@RequestBody SignInResource signInResource) {
        var signInCommand = SignInCommandFromResourceAssembler.toCommandFromResource(signInResource);
        var authenticatedUser = userCommandService.handle(signInCommand);
        if (authenticatedUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var tokens = authenticatedUser.get().tokens();
        var authenticatedUserResource = AuthenticatedUserResourceFromEntityAssembler
                .toResourceFromEntity(authenticatedUser.get().user(), tokens);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.build(tokens.refreshToken()).toString())
                .body(authenticatedUserResource);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<UserResource> signUp(@Valid @RequestBody SignUpResource signUpResource) {
        var signUpCommand = SignUpCommandFromResourceAssembler.toCommandFromResource(signUpResource);
        var user = userCommandService.handle(signUpCommand);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return new ResponseEntity<>(userResource, HttpStatus.CREATED);
    }

    @PostMapping("/refresh")
    @Parameter(in = ParameterIn.HEADER, name = "X-Requested-With", required = true,
            description = CSRF_HEADER_DESCRIPTION,
            schema = @Schema(type = "string", allowableValues = "XMLHttpRequest", defaultValue = "XMLHttpRequest"))
    public ResponseEntity<AccessTokenResource> refresh(
            @Parameter(hidden = true)
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var tokens = userCommandService.handle(new RefreshTokenCommand(refreshToken));
        if (tokens.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.expired().toString())
                    .build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookieFactory.build(tokens.get().refreshToken()).toString())
                .body(new AccessTokenResource(tokens.get().accessToken()));
    }

    @PostMapping("/sign-out")
    @Parameter(in = ParameterIn.HEADER, name = "X-Requested-With", required = true,
            description = CSRF_HEADER_DESCRIPTION,
            schema = @Schema(type = "string", allowableValues = "XMLHttpRequest", defaultValue = "XMLHttpRequest"))
    public ResponseEntity<Void> signOut(
            @Parameter(hidden = true)
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null) {
            userCommandService.handle(new SignOutCommand(refreshToken));
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.expired().toString())
                .build();
    }

}

package com.jacobo.cinekt.iam.interfaces;

import com.jacobo.cinekt.iam.domain.model.commands.RefreshTokenCommand;
import com.jacobo.cinekt.iam.domain.model.commands.SignOutCommand;
import com.jacobo.cinekt.iam.interfaces.rest.resources.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacobo.cinekt.iam.domain.services.UserCommandService;
import com.jacobo.cinekt.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.jacobo.cinekt.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.jacobo.cinekt.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.jacobo.cinekt.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthenticationController {
    private final UserCommandService userCommandService;

    public AuthenticationController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthenticatedUserResource> signIn(@RequestBody SignInResource signInResource) {
        var signInCommand = SignInCommandFromResourceAssembler.toCommandFromResource(signInResource);
        var authenticatedUser = userCommandService.handle(signInCommand);
        if (authenticatedUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var authenticatedUserResource = AuthenticatedUserResourceFromEntityAssembler
                .toResourceFromEntity(authenticatedUser.get().user(), authenticatedUser.get().tokens());
        return ResponseEntity.ok(authenticatedUserResource);
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
    public ResponseEntity<AccessTokenResource> refresh(@RequestBody RefreshTokenResource refreshTokenResource) {
        var command = new RefreshTokenCommand(refreshTokenResource.refreshToken());
        var accessToken = userCommandService.handle(command);
        if (accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new AccessTokenResource(accessToken.get()));
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(@RequestBody RefreshTokenResource refreshTokenResource){
        var command = new SignOutCommand(refreshTokenResource.refreshToken());
        userCommandService.handle(command);
        return ResponseEntity.noContent().build();
    }

}

package com.jacobo.cinekt.iam.interfaces;

import java.util.List;

import com.jacobo.cinekt.iam.domain.model.commands.ChangePasswordCommand;
import com.jacobo.cinekt.iam.domain.model.commands.ChangeUserRoleCommand;
import com.jacobo.cinekt.iam.domain.services.UserCommandService;
import com.jacobo.cinekt.iam.interfaces.rest.cookies.RefreshTokenCookieFactory;
import com.jacobo.cinekt.iam.interfaces.rest.resources.AccessTokenResource;
import com.jacobo.cinekt.iam.interfaces.rest.resources.ChangePasswordResource;
import com.jacobo.cinekt.iam.interfaces.rest.resources.ChangeUserRoleResource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jacobo.cinekt.iam.domain.model.queries.GetAllUsersQuery;
import com.jacobo.cinekt.iam.domain.model.queries.GetUserByIdQuery;
import com.jacobo.cinekt.iam.domain.services.UserQueryService;
import com.jacobo.cinekt.iam.interfaces.rest.resources.UserResource;
import com.jacobo.cinekt.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Users", description = "User Management Endpoints")
public class UsersController {
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public UsersController(UserQueryService userQueryService, UserCommandService userCommandService,
            RefreshTokenCookieFactory refreshTokenCookieFactory) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResource>> getAllUsers() {
        var getAllUsersQuery = new GetAllUsersQuery();
        var users = userQueryService.handle(getAllUsersQuery);
        var userResources = users.stream().map(UserResourceFromEntityAssembler::toResourceFromEntity).toList();
        return ResponseEntity.ok(userResources);
    }

    @GetMapping(value = "/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
        var getUserByIdQuery = new GetUserByIdQuery(userId);
        var user = userQueryService.handle(getUserByIdQuery);
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return ResponseEntity.ok(userResource);
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserRole(@PathVariable Long userId, @Valid @RequestBody ChangeUserRoleResource resource) {
        var command = new ChangeUserRoleCommand(userId, resource.role());
        userCommandService.handle(command);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/password")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<AccessTokenResource> changePassword(@PathVariable Long userId,
            @Valid @RequestBody ChangePasswordResource resource) {
        var command = new ChangePasswordCommand(userId, resource.currentPassword(), resource.newPassword());
        var tokens = userCommandService.handle(command);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.build(tokens.refreshToken()).toString())
                .body(new AccessTokenResource(tokens.accessToken()));
    }

}

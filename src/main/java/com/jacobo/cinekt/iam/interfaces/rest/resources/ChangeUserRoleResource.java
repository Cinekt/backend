package com.jacobo.cinekt.iam.interfaces.rest.resources;

import com.jacobo.cinekt.iam.domain.model.valueobjects.Roles;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleResource(@NotNull Roles role) {
}

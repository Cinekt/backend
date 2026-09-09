package com.jacobo.cinekt.iam.domain.model.commands;

import com.jacobo.cinekt.iam.domain.model.valueobjects.Roles;

public record ChangeUserRoleCommand(Long userId, Roles role) {
}

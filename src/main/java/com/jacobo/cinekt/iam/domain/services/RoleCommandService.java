package com.jacobo.cinekt.iam.domain.services;

import com.jacobo.cinekt.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
    void handle(SeedRolesCommand command);
}

package com.jacobo.platform.iam.domain.services;

import com.jacobo.platform.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
    void handle(SeedRolesCommand command);
}

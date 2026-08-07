package com.jacobo.cinekt.iam.interfaces.rest.transform;

import com.jacobo.cinekt.iam.domain.model.entities.Role;
import com.jacobo.cinekt.iam.interfaces.rest.resources.RoleResource;

public class RoleResourceFromEntityAssembler {
    public static RoleResource toResourceFromEntity(Role role) {
        return new RoleResource(role.getId(), role.getStringName());
    }
}

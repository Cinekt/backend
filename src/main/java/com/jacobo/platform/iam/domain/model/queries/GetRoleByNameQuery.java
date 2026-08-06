package com.jacobo.platform.iam.domain.model.queries;

import com.jacobo.platform.iam.domain.model.valueobjects.Roles;

public record GetRoleByNameQuery(Roles roleName) {
}

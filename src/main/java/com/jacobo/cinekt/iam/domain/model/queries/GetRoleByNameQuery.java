package com.jacobo.cinekt.iam.domain.model.queries;

import com.jacobo.cinekt.iam.domain.model.valueobjects.Roles;

public record GetRoleByNameQuery(Roles roleName) {
}

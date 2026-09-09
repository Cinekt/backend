package com.jacobo.cinekt.iam.domain.services;

import java.util.List;
import java.util.Optional;

import com.jacobo.cinekt.iam.domain.model.aggregates.User;
import com.jacobo.cinekt.iam.domain.model.queries.GetAllUsersQuery;
import com.jacobo.cinekt.iam.domain.model.queries.GetUserByIdQuery;
import com.jacobo.cinekt.iam.domain.model.queries.GetUserByEmailQuery;

public interface UserQueryService {
    List<User> handle(GetAllUsersQuery query);

    Optional<User> handle(GetUserByIdQuery query);

    Optional<User> handle(GetUserByEmailQuery query);
}

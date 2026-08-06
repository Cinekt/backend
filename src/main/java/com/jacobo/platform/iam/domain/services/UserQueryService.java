package com.jacobo.platform.iam.domain.services;

import java.util.List;
import java.util.Optional;

import com.jacobo.platform.iam.domain.model.aggregates.User;
import com.jacobo.platform.iam.domain.model.queries.GetAllUsersQuery;
import com.jacobo.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.jacobo.platform.iam.domain.model.queries.GetUserByUsernameQuery;

public interface UserQueryService {
    List<User> handle(GetAllUsersQuery query);

    Optional<User> handle(GetUserByIdQuery query);

    Optional<User> handle(GetUserByUsernameQuery query);
}

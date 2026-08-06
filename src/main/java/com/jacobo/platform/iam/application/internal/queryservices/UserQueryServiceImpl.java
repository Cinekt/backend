package com.jacobo.platform.iam.application.internal.queryservices;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jacobo.platform.iam.domain.model.aggregates.User;
import com.jacobo.platform.iam.domain.model.queries.GetAllUsersQuery;
import com.jacobo.platform.iam.domain.model.queries.GetUserByIdQuery;
import com.jacobo.platform.iam.domain.model.queries.GetUserByUsernameQuery;
import com.jacobo.platform.iam.domain.services.UserQueryService;
import com.jacobo.platform.iam.infrastructure.persistence.jpa.repositories.UserRepository;

@Service
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;

    public UserQueryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> handle(GetAllUsersQuery query) {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId());
    }

    @Override
    public Optional<User> handle(GetUserByUsernameQuery query) {
        return userRepository.findByUsername(query.username());
    }
}

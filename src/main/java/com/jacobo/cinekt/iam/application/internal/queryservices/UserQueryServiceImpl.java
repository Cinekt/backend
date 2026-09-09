package com.jacobo.cinekt.iam.application.internal.queryservices;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jacobo.cinekt.iam.domain.model.aggregates.User;
import com.jacobo.cinekt.iam.domain.model.queries.GetAllUsersQuery;
import com.jacobo.cinekt.iam.domain.model.queries.GetUserByIdQuery;
import com.jacobo.cinekt.iam.domain.model.queries.GetUserByEmailQuery;
import com.jacobo.cinekt.iam.domain.services.UserQueryService;
import com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories.UserRepository;

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
    public Optional<User> handle(GetUserByEmailQuery query) {
        return userRepository.findByEmail(query.email());
    }
}

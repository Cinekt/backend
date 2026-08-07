package com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jacobo.cinekt.iam.domain.model.entities.Role;
import com.jacobo.cinekt.iam.domain.model.valueobjects.Roles;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(Roles roleName);

    boolean existsByName(Roles Name);
}

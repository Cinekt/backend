package com.jacobo.cinekt.iam.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.jacobo.cinekt.iam.domain.model.valueobjects.Roles;
import com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.jacobo.cinekt.iam.infrastructure.persistence.jpa.repositories.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * Guards against a regression of P2: {@code User.roles} used to cascade {@code REMOVE} onto the
 * shared {@code Role} catalog. Deleting a user must never delete a role that other users may
 * still be referencing.
 */
@SpringBootTest
@Transactional
class UserRoleCascadeTests {

    private static final String TEST_USER_EMAIL = "cascade-test-user@cinekt.local";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deletingAUserDoesNotDeleteItsRoleFromTheCatalog() {
        var userRole = roleRepository.findByName(Roles.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER should have been seeded on startup"));

        var testUser = new User(TEST_USER_EMAIL, "irrelevant-hash");
        testUser.addRole(userRole);
        userRepository.saveAndFlush(testUser);

        userRepository.delete(testUser);
        entityManager.flush();
        entityManager.clear();

        assertThat(roleRepository.findByName(Roles.ROLE_USER)).isPresent();
        assertThat(userRepository.findByEmail(TEST_USER_EMAIL)).isEmpty();
    }

}

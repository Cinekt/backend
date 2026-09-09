package com.jacobo.cinekt.iam.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.jacobo.cinekt.iam.domain.model.valueobjects.Roles;

class RoleTest {

    @Nested
    class GetDefaultRole {

        @Test
        void alwaysReturnsRoleUser() {
            var defaultRole = Role.getDefaultRole();

            assertThat(defaultRole.getName()).isEqualTo(Roles.ROLE_USER);
        }
    }

    @Nested
    class ToRoleFromName {

        @Test
        void buildsARoleFromAValidEnumName() {
            var role = Role.toRoleFromName("ROLE_ADMIN");

            assertThat(role.getName()).isEqualTo(Roles.ROLE_ADMIN);
        }

        @Test
        void throwsWhenTheNameDoesNotMatchAnyKnownRole() {
            assertThatThrownBy(() -> Role.toRoleFromName("NOT_A_ROLE"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ValidateRoleSet {

        @Test
        void returnsTheDefaultRoleWhenGivenNull() {
            var roles = Role.validateRoleSet(null);

            assertThat(roles).extracting(Role::getName).containsExactly(Roles.ROLE_USER);
        }

        @Test
        void returnsTheDefaultRoleWhenGivenAnEmptyList() {
            var roles = Role.validateRoleSet(List.of());

            assertThat(roles).extracting(Role::getName).containsExactly(Roles.ROLE_USER);
        }

        @Test
        void returnsTheGivenListUnchangedWhenItIsNotEmpty() {
            var adminRole = new Role(Roles.ROLE_ADMIN);
            var given = List.of(adminRole);

            var roles = Role.validateRoleSet(given);

            assertThat(roles).containsExactly(adminRole);
        }
    }
}

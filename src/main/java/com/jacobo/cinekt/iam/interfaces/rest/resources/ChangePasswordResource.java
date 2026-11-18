package com.jacobo.cinekt.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordResource(@NotBlank String currentPassword,
        @NotBlank @Size(max = 120) String newPassword) {
}

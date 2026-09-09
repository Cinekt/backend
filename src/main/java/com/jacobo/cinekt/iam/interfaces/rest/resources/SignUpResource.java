package com.jacobo.cinekt.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SignUpResource(@NotBlank @Email @Size(max=50) String email,@NotBlank @Size(max=120) String password) {
}

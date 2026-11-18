package com.jacobo.cinekt.iam.domain.model.commands;

public record ChangePasswordCommand(Long userId, String currentPassword, String newPassword) {
}

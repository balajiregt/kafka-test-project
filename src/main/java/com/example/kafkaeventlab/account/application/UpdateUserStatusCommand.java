package com.example.kafkaeventlab.account.application;

import com.example.kafkaeventlab.account.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusCommand(
        @NotNull UserStatus status,
        @NotBlank String reason
) {
}

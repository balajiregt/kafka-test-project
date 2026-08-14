package com.balaji.kafkaeventlab.identity.application;

import com.balaji.kafkaeventlab.identity.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusCommand(
        @NotNull UserStatus status,
        @NotBlank String reason
) {
}

package com.balaji.kafkaeventlab.account.application;

import com.balaji.kafkaeventlab.account.domain.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusCommand(
        @NotNull UserStatus status,
        @NotBlank String reason
) {
}

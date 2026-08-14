package com.balaji.kafkaeventlab.identity.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RevokeApplicationAccessCommand(
        @NotNull UUID accessId,
        @NotBlank String revokedBy,
        @NotBlank String reason
) {
}

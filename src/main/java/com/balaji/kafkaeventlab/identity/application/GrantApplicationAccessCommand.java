package com.balaji.kafkaeventlab.identity.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record GrantApplicationAccessCommand(
        @NotNull UUID userId,
        @NotNull UUID applicationId,
        @NotBlank String roleName,
        @NotBlank String grantedBy,
        Instant expiresAt
) {
}

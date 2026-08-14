package com.balaji.kafkaeventlab.identity.application;

import com.balaji.kafkaeventlab.identity.domain.CredentialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterCredentialCommand(
        @NotNull UUID userId,
        @NotNull CredentialType credentialType,
        @NotBlank String credentialReference,
        boolean mfaEnabled
) {
}

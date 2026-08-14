package com.balaji.kafkaeventlab.account.application;

import com.balaji.kafkaeventlab.account.domain.CredentialType;
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

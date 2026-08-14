package com.balaji.kafkaeventlab.account.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompletePasswordResetCommand(
        @NotNull UUID resetRequestId,
        @NotBlank String newCredentialReference,
        boolean mfaEnabled
) {
}

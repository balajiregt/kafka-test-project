package com.balaji.kafkaeventlab.account.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignInCommand(
        @Email @NotBlank String email,
        @NotBlank String credentialReference
) {
}

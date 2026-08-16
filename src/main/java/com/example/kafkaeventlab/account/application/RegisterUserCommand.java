package com.example.kafkaeventlab.account.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserCommand(
        @Email @NotBlank String email,
        @NotBlank String displayName,
        @NotBlank String department,
        @NotBlank String jobTitle
) {
}

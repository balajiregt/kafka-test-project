package com.example.kafkaeventlab.account.application;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileCommand(
        @NotBlank String displayName,
        @NotBlank String department,
        @NotBlank String jobTitle
) {
}

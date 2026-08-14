package com.balaji.kafkaeventlab.identity.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CreateApplicationCommand(
        @NotBlank String applicationCode,
        @NotBlank String displayName,
        @NotBlank String ownerTeam,
        @NotEmpty Set<@NotBlank String> defaultRoles
) {
}

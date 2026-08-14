package com.balaji.kafkaeventlab.identity.domain;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "identity_applications")
public class IdentityApplication {

    @Id
    private UUID applicationId;

    private String applicationCode;
    private String displayName;
    private String ownerTeam;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> defaultRoles = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private Instant createdAt;

    protected IdentityApplication() {
    }

    private IdentityApplication(
            UUID applicationId,
            String applicationCode,
            String displayName,
            String ownerTeam,
            Set<String> defaultRoles,
            ApplicationStatus status,
            Instant createdAt
    ) {
        this.applicationId = applicationId;
        this.applicationCode = applicationCode;
        this.displayName = displayName;
        this.ownerTeam = ownerTeam;
        this.defaultRoles = defaultRoles;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static IdentityApplication create(
            String applicationCode,
            String displayName,
            String ownerTeam,
            Set<String> defaultRoles
    ) {
        return new IdentityApplication(
                UUID.randomUUID(),
                applicationCode,
                displayName,
                ownerTeam,
                new LinkedHashSet<>(defaultRoles),
                ApplicationStatus.ACTIVE,
                Instant.now()
        );
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public Set<String> getDefaultRoles() {
        return defaultRoles;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.balaji.kafkaeventlab.identity.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_user_credentials")
public class UserCredential {

    @Id
    private UUID credentialId;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private CredentialType credentialType;

    private String credentialReference;

    @Enumerated(EnumType.STRING)
    private CredentialStatus status;

    private boolean mfaEnabled;
    private Instant createdAt;
    private Instant updatedAt;

    protected UserCredential() {
    }

    private UserCredential(
            UUID credentialId,
            UUID userId,
            CredentialType credentialType,
            String credentialReference,
            CredentialStatus status,
            boolean mfaEnabled,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.credentialId = credentialId;
        this.userId = userId;
        this.credentialType = credentialType;
        this.credentialReference = credentialReference;
        this.status = status;
        this.mfaEnabled = mfaEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserCredential register(
            UUID userId,
            CredentialType credentialType,
            String credentialReference,
            boolean mfaEnabled
    ) {
        Instant now = Instant.now();
        return new UserCredential(
                UUID.randomUUID(),
                userId,
                credentialType,
                credentialReference,
                CredentialStatus.ACTIVE,
                mfaEnabled,
                now,
                now
        );
    }

    public UUID getCredentialId() {
        return credentialId;
    }

    public UUID getUserId() {
        return userId;
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }

    public String getCredentialReference() {
        return credentialReference;
    }

    public CredentialStatus getStatus() {
        return status;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

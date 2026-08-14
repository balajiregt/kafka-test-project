package com.balaji.kafkaeventlab.identity.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_application_access_grants")
public class ApplicationAccessGrant {

    @Id
    private UUID accessId;

    private UUID userId;
    private UUID applicationId;
    private String roleName;
    private String grantedBy;
    private Instant grantedAt;
    private Instant expiresAt;
    private String revokedBy;
    private String revocationReason;
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    private AccessStatus status;

    protected ApplicationAccessGrant() {
    }

    private ApplicationAccessGrant(
            UUID accessId,
            UUID userId,
            UUID applicationId,
            String roleName,
            String grantedBy,
            Instant grantedAt,
            Instant expiresAt,
            AccessStatus status
    ) {
        this.accessId = accessId;
        this.userId = userId;
        this.applicationId = applicationId;
        this.roleName = roleName;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public static ApplicationAccessGrant grant(
            UUID userId,
            UUID applicationId,
            String roleName,
            String grantedBy,
            Instant expiresAt
    ) {
        return new ApplicationAccessGrant(
                UUID.randomUUID(),
                userId,
                applicationId,
                roleName,
                grantedBy,
                Instant.now(),
                expiresAt,
                AccessStatus.ACTIVE
        );
    }

    public void revoke(String revokedBy, String reason) {
        this.status = AccessStatus.REVOKED;
        this.revokedBy = revokedBy;
        this.revocationReason = reason;
        this.revokedAt = Instant.now();
    }

    public UUID getAccessId() {
        return accessId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public AccessStatus getStatus() {
        return status;
    }
}

package com.balaji.kafkaeventlab.identity.projection;

import com.balaji.kafkaeventlab.identity.domain.AccessStatus;
import com.balaji.kafkaeventlab.identity.domain.ApplicationStatus;
import com.balaji.kafkaeventlab.identity.domain.UserStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_user_access_summaries")
public class UserAccessSummaryProjection {

    @Id
    private UUID accessId;

    private UUID userId;
    private String email;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    private UUID applicationId;
    private String applicationCode;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus;

    private String roleName;

    @Enumerated(EnumType.STRING)
    private AccessStatus accessStatus;

    private Instant lastUpdatedAt;

    protected UserAccessSummaryProjection() {
    }

    public UserAccessSummaryProjection(
            UUID accessId,
            UUID userId,
            String email,
            UserStatus userStatus,
            UUID applicationId,
            String applicationCode,
            ApplicationStatus applicationStatus,
            String roleName,
            AccessStatus accessStatus,
            Instant lastUpdatedAt
    ) {
        this.accessId = accessId;
        this.userId = userId;
        this.email = email;
        this.userStatus = userStatus;
        this.applicationId = applicationId;
        this.applicationCode = applicationCode;
        this.applicationStatus = applicationStatus;
        this.roleName = roleName;
        this.accessStatus = accessStatus;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void changeUserStatus(UserStatus userStatus, Instant lastUpdatedAt) {
        this.userStatus = userStatus;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void revoke(Instant lastUpdatedAt) {
        this.accessStatus = AccessStatus.REVOKED;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public UUID getAccessId() {
        return accessId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public ApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public String getRoleName() {
        return roleName;
    }

    public AccessStatus getAccessStatus() {
        return accessStatus;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}

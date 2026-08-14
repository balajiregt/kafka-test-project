package com.balaji.kafkaeventlab.account.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_password_reset_requests")
public class PasswordResetRequest {

    @Id
    private UUID resetRequestId;

    private UUID userId;
    private String email;

    @Enumerated(EnumType.STRING)
    private PasswordResetStatus status;

    private Instant requestedAt;
    private Instant completedAt;

    protected PasswordResetRequest() {
    }

    private PasswordResetRequest(
            UUID resetRequestId,
            UUID userId,
            String email,
            PasswordResetStatus status,
            Instant requestedAt
    ) {
        this.resetRequestId = resetRequestId;
        this.userId = userId;
        this.email = email;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public static PasswordResetRequest request(UUID userId, String email) {
        return new PasswordResetRequest(UUID.randomUUID(), userId, email, PasswordResetStatus.REQUESTED, Instant.now());
    }

    public void complete() {
        this.status = PasswordResetStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public UUID getResetRequestId() {
        return resetRequestId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public PasswordResetStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}

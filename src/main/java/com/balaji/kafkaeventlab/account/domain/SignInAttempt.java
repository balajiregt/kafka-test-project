package com.balaji.kafkaeventlab.account.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_sign_in_attempts")
public class SignInAttempt {

    @Id
    private UUID signInAttemptId;

    private UUID userId;
    private String email;

    @Enumerated(EnumType.STRING)
    private AuthenticationResult result;

    private String reason;
    private Instant attemptedAt;

    protected SignInAttempt() {
    }

    private SignInAttempt(
            UUID signInAttemptId,
            UUID userId,
            String email,
            AuthenticationResult result,
            String reason,
            Instant attemptedAt
    ) {
        this.signInAttemptId = signInAttemptId;
        this.userId = userId;
        this.email = email;
        this.result = result;
        this.reason = reason;
        this.attemptedAt = attemptedAt;
    }

    public static SignInAttempt success(UUID userId, String email) {
        return new SignInAttempt(UUID.randomUUID(), userId, email, AuthenticationResult.SUCCESS, "Credential accepted", Instant.now());
    }

    public static SignInAttempt failure(UUID userId, String email, String reason) {
        return new SignInAttempt(UUID.randomUUID(), userId, email, AuthenticationResult.FAILURE, reason, Instant.now());
    }

    public UUID getSignInAttemptId() {
        return signInAttemptId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public AuthenticationResult getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}

package com.balaji.kafkaeventlab.account.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AccountEvent(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        UUID subjectUserId,
        String correlationId,
        Instant occurredAt,
        Integer schemaVersion,
        Map<String, Object> data
) {
    public static AccountEvent userRegistered(
            UUID userId,
            String email,
            String displayName,
            String department,
            String jobTitle,
            String status
    ) {
        return create(
                "USER_REGISTERED",
                "USER",
                userId,
                userId,
                Map.of(
                        "email", email,
                        "displayName", displayName,
                        "department", department,
                        "jobTitle", jobTitle,
                        "status", status
                )
        );
    }

    public static AccountEvent userStatusChanged(UUID userId, String status, String reason) {
        return create(
                "USER_STATUS_CHANGED",
                "USER",
                userId,
                userId,
                Map.of("status", status, "reason", reason)
        );
    }

    public static AccountEvent userProfileUpdated(
            UUID userId,
            String email,
            String displayName,
            String department,
            String jobTitle
    ) {
        return create(
                "USER_PROFILE_UPDATED",
                "USER",
                userId,
                userId,
                Map.of(
                        "email", email,
                        "displayName", displayName,
                        "department", department,
                        "jobTitle", jobTitle
                )
        );
    }

    public static AccountEvent credentialRegistered(
            UUID credentialId,
            UUID userId,
            String credentialType,
            String credentialReference,
            String status,
            boolean mfaEnabled
    ) {
        return create(
                "CREDENTIAL_REGISTERED",
                "CREDENTIAL",
                credentialId,
                userId,
                Map.of(
                        "credentialType", credentialType,
                        "credentialReference", credentialReference,
                        "status", status,
                        "mfaEnabled", mfaEnabled
                )
        );
    }

    public static AccountEvent signInSucceeded(UUID signInAttemptId, UUID userId, String email) {
        return create(
                "SIGN_IN_SUCCEEDED",
                "SIGN_IN_ATTEMPT",
                signInAttemptId,
                userId,
                Map.of("email", email, "result", "SUCCESS", "reason", "Credential accepted")
        );
    }

    public static AccountEvent signInFailed(UUID signInAttemptId, UUID userId, String email, String reason) {
        return create(
                "SIGN_IN_FAILED",
                "SIGN_IN_ATTEMPT",
                signInAttemptId,
                userId,
                Map.of("email", email, "result", "FAILURE", "reason", reason)
        );
    }

    public static AccountEvent passwordResetRequested(UUID resetRequestId, UUID userId, String email) {
        return create(
                "PASSWORD_RESET_REQUESTED",
                "PASSWORD_RESET",
                resetRequestId,
                userId,
                Map.of("email", email, "status", "REQUESTED")
        );
    }

    public static AccountEvent passwordResetCompleted(
            UUID resetRequestId,
            UUID userId,
            String email,
            UUID credentialId,
            boolean mfaEnabled
    ) {
        return create(
                "PASSWORD_RESET_COMPLETED",
                "PASSWORD_RESET",
                resetRequestId,
                userId,
                Map.of(
                        "email", email,
                        "credentialId", credentialId.toString(),
                        "credentialType", "PASSWORD_HASH",
                        "status", "COMPLETED",
                        "mfaEnabled", mfaEnabled
                )
        );
    }

    private static AccountEvent create(
            String eventType,
            String aggregateType,
            UUID aggregateId,
            UUID subjectUserId,
            Map<String, Object> data
    ) {
        return new AccountEvent(
                UUID.randomUUID(),
                eventType,
                aggregateType,
                aggregateId,
                subjectUserId,
                UUID.randomUUID().toString(),
                Instant.now(),
                1,
                new LinkedHashMap<>(data)
        );
    }
}

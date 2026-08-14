package com.balaji.kafkaeventlab.identity.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record IdentityEvent(
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
    public static IdentityEvent userRegistered(
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

    public static IdentityEvent userStatusChanged(UUID userId, String status, String reason) {
        return create(
                "USER_STATUS_CHANGED",
                "USER",
                userId,
                userId,
                Map.of("status", status, "reason", reason)
        );
    }

    public static IdentityEvent credentialRegistered(
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

    public static IdentityEvent applicationCreated(
            UUID applicationId,
            String applicationCode,
            String displayName,
            String ownerTeam,
            String status
    ) {
        return create(
                "APPLICATION_CREATED",
                "APPLICATION",
                applicationId,
                null,
                Map.of(
                        "applicationCode", applicationCode,
                        "displayName", displayName,
                        "ownerTeam", ownerTeam,
                        "status", status
                )
        );
    }

    public static IdentityEvent accessGranted(
            UUID accessId,
            UUID userId,
            UUID applicationId,
            String email,
            String userStatus,
            String applicationCode,
            String applicationStatus,
            String roleName,
            String grantedBy
    ) {
        return create(
                "APPLICATION_ACCESS_GRANTED",
                "ACCESS_GRANT",
                accessId,
                userId,
                Map.of(
                        "applicationId", applicationId.toString(),
                        "email", email,
                        "userStatus", userStatus,
                        "applicationCode", applicationCode,
                        "applicationStatus", applicationStatus,
                        "roleName", roleName,
                        "grantedBy", grantedBy,
                        "accessStatus", "ACTIVE"
                )
        );
    }

    public static IdentityEvent accessRevoked(
            UUID accessId,
            UUID userId,
            UUID applicationId,
            String revokedBy,
            String reason
    ) {
        return create(
                "APPLICATION_ACCESS_REVOKED",
                "ACCESS_GRANT",
                accessId,
                userId,
                Map.of(
                        "applicationId", applicationId.toString(),
                        "revokedBy", revokedBy,
                        "reason", reason,
                        "accessStatus", "REVOKED"
                )
        );
    }

    private static IdentityEvent create(
            String eventType,
            String aggregateType,
            UUID aggregateId,
            UUID subjectUserId,
            Map<String, Object> data
    ) {
        return new IdentityEvent(
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

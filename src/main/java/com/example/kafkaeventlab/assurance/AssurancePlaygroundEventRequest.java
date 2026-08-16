package com.example.kafkaeventlab.assurance;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AssurancePlaygroundEventRequest(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        UUID subjectId,
        String correlationId,
        String topicName,
        Integer partitionNumber,
        Long offsetNumber,
        Instant occurredAt,
        Instant consumedAt,
        Integer schemaVersion,
        Map<String, Object> data,
        String eventJson
) {
}

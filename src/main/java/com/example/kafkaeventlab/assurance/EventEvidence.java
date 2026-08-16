package com.example.kafkaeventlab.assurance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_evidence")
public class EventEvidence {

    @Id
    private UUID eventId;

    private String eventType;
    private String aggregateType;
    private UUID aggregateId;
    private UUID subjectId;
    private String correlationId;
    private String topicName;
    private Integer partitionNumber;
    private Long offsetNumber;
    private Instant occurredAt;
    private Instant consumedAt;
    private Integer schemaVersion;

    @Column(length = 6000)
    private String eventJson;

    protected EventEvidence() {
    }

    public EventEvidence(
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
            String eventJson
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.subjectId = subjectId;
        this.correlationId = correlationId;
        this.topicName = topicName;
        this.partitionNumber = partitionNumber;
        this.offsetNumber = offsetNumber;
        this.occurredAt = occurredAt;
        this.consumedAt = consumedAt;
        this.schemaVersion = schemaVersion;
        this.eventJson = eventJson;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getTopicName() {
        return topicName;
    }

    public Integer getPartitionNumber() {
        return partitionNumber;
    }

    public Long getOffsetNumber() {
        return offsetNumber;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public String getEventJson() {
        return eventJson;
    }
}

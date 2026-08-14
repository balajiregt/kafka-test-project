package com.balaji.kafkaeventlab.account.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_tracked_events")
public class AccountTrackedEvent {

    @Id
    private UUID eventId;

    private String eventType;
    private String aggregateType;
    private UUID aggregateId;
    private UUID subjectUserId;
    private String correlationId;
    private String topicName;
    private Integer partitionNumber;
    private Long offsetNumber;
    private Instant occurredAt;
    private Instant consumedAt;

    @Column(length = 6000)
    private String eventJson;

    protected AccountTrackedEvent() {
    }

    public AccountTrackedEvent(
            UUID eventId,
            String eventType,
            String aggregateType,
            UUID aggregateId,
            UUID subjectUserId,
            String correlationId,
            String topicName,
            Integer partitionNumber,
            Long offsetNumber,
            Instant occurredAt,
            Instant consumedAt,
            String eventJson
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.subjectUserId = subjectUserId;
        this.correlationId = correlationId;
        this.topicName = topicName;
        this.partitionNumber = partitionNumber;
        this.offsetNumber = offsetNumber;
        this.occurredAt = occurredAt;
        this.consumedAt = consumedAt;
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

    public UUID getSubjectUserId() {
        return subjectUserId;
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

    public String getEventJson() {
        return eventJson;
    }
}

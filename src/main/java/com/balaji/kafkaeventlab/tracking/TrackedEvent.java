package com.balaji.kafkaeventlab.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class TrackedEvent {

    @Id
    private UUID eventId;

    private String eventType;
    private UUID orderId;
    private String topicName;
    private Integer partitionNumber;
    private Long offsetNumber;
    private Instant occurredAt;
    private Instant consumedAt;

    @Column(length = 4000)
    private String eventJson;

    protected TrackedEvent() {
    }

    public TrackedEvent(
            UUID eventId,
            String eventType,
            UUID orderId,
            String topicName,
            Integer partitionNumber,
            Long offsetNumber,
            Instant occurredAt,
            Instant consumedAt,
            String eventJson
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.orderId = orderId;
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

    public UUID getOrderId() {
        return orderId;
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

package com.balaji.kafkaeventlab.identity.tracking;

import com.balaji.kafkaeventlab.identity.domain.AccessStatus;
import com.balaji.kafkaeventlab.identity.domain.ApplicationStatus;
import com.balaji.kafkaeventlab.identity.domain.UserStatus;
import com.balaji.kafkaeventlab.identity.event.IdentityEvent;
import com.balaji.kafkaeventlab.identity.projection.UserAccessSummaryProjection;
import com.balaji.kafkaeventlab.identity.projection.UserAccessSummaryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class IdentityEventConsumer {

    private final IdentityTrackedEventRepository trackedEventRepository;
    private final UserAccessSummaryRepository accessSummaryRepository;
    private final ObjectMapper objectMapper;

    public IdentityEventConsumer(
            IdentityTrackedEventRepository trackedEventRepository,
            UserAccessSummaryRepository accessSummaryRepository,
            ObjectMapper objectMapper
    ) {
        this.trackedEventRepository = trackedEventRepository;
        this.accessSummaryRepository = accessSummaryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.identity-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, IdentityEvent> record) throws JsonProcessingException {
        IdentityEvent event = record.value();
        trackedEventRepository.save(new IdentityTrackedEvent(
                event.eventId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                event.subjectUserId(),
                event.correlationId(),
                record.topic(),
                record.partition(),
                record.offset(),
                event.occurredAt(),
                Instant.now(),
                objectMapper.writeValueAsString(event)
        ));

        updateAccessSummary(event);
    }

    private void updateAccessSummary(IdentityEvent event) {
        if ("APPLICATION_ACCESS_GRANTED".equals(event.eventType())) {
            Map<String, Object> data = event.data();
            accessSummaryRepository.save(new UserAccessSummaryProjection(
                    event.aggregateId(),
                    event.subjectUserId(),
                    data.get("email").toString(),
                    UserStatus.valueOf(data.get("userStatus").toString()),
                    UUID.fromString(data.get("applicationId").toString()),
                    data.get("applicationCode").toString(),
                    ApplicationStatus.valueOf(data.get("applicationStatus").toString()),
                    data.get("roleName").toString(),
                    AccessStatus.valueOf(data.get("accessStatus").toString()),
                    event.occurredAt()
            ));
            return;
        }

        if ("APPLICATION_ACCESS_REVOKED".equals(event.eventType())) {
            accessSummaryRepository.findById(event.aggregateId()).ifPresent(summary -> {
                summary.revoke(event.occurredAt());
                accessSummaryRepository.save(summary);
            });
            return;
        }

        if ("USER_STATUS_CHANGED".equals(event.eventType())) {
            UserStatus status = UserStatus.valueOf(event.data().get("status").toString());
            accessSummaryRepository.findByUserId(event.subjectUserId()).forEach(summary -> {
                summary.changeUserStatus(status, event.occurredAt());
                accessSummaryRepository.save(summary);
            });
        }
    }
}

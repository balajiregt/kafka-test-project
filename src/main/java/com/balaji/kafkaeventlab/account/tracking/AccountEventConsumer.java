package com.balaji.kafkaeventlab.account.tracking;

import com.balaji.kafkaeventlab.account.event.AccountEvent;
import com.balaji.kafkaeventlab.assurance.EventEvidence;
import com.balaji.kafkaeventlab.assurance.EventEvidenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
@Component
public class AccountEventConsumer {

    private final EventEvidenceRepository eventEvidenceRepository;
    private final ObjectMapper objectMapper;

    public AccountEventConsumer(
            EventEvidenceRepository eventEvidenceRepository,
            ObjectMapper objectMapper
    ) {
        this.eventEvidenceRepository = eventEvidenceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.account-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, AccountEvent> record) throws JsonProcessingException {
        AccountEvent event = record.value();
        eventEvidenceRepository.save(new EventEvidence(
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
                event.schemaVersion(),
                objectMapper.writeValueAsString(event)
        ));
    }
}

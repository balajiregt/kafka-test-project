package com.balaji.kafkaeventlab.account.tracking;

import com.balaji.kafkaeventlab.account.event.AccountEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
@Component
public class AccountEventConsumer {

    private final AccountTrackedEventRepository trackedEventRepository;
    private final ObjectMapper objectMapper;

    public AccountEventConsumer(
            AccountTrackedEventRepository trackedEventRepository,
            ObjectMapper objectMapper
    ) {
        this.trackedEventRepository = trackedEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.account-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, AccountEvent> record) throws JsonProcessingException {
        AccountEvent event = record.value();
        trackedEventRepository.save(new AccountTrackedEvent(
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
    }
}

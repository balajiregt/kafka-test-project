package com.balaji.kafkaeventlab.account.event;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class AccountEventPublisher {

    private final KafkaTemplate<String, AccountEvent> kafkaTemplate;
    private final String topicName;

    public AccountEventPublisher(
            KafkaTemplate<String, AccountEvent> kafkaTemplate,
            @Value("${app.kafka.account-events-topic}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public CompletableFuture<AccountEvent> publish(AccountEvent event) {
        ProducerRecord<String, AccountEvent> record = new ProducerRecord<>(
                topicName,
                event.aggregateId().toString(),
                event
        );
        record.headers().add(new RecordHeader("eventType", event.eventType().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("correlationId", event.correlationId().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader("schemaVersion", event.schemaVersion().toString().getBytes(StandardCharsets.UTF_8)));
        return kafkaTemplate.send(record).thenApply(sendResult -> event);
    }
}

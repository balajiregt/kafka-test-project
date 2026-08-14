package com.balaji.kafkaeventlab.identity.event;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class IdentityEventPublisher {

    private final KafkaTemplate<String, IdentityEvent> kafkaTemplate;
    private final String topicName;

    public IdentityEventPublisher(
            KafkaTemplate<String, IdentityEvent> kafkaTemplate,
            @Value("${app.kafka.identity-events-topic}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public CompletableFuture<IdentityEvent> publish(IdentityEvent event) {
        ProducerRecord<String, IdentityEvent> record = new ProducerRecord<>(
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

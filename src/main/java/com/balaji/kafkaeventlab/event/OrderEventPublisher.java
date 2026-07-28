package com.balaji.kafkaeventlab.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final String topicName;

    public OrderEventPublisher(
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            @Value("${app.kafka.order-events-topic}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public CompletableFuture<OrderEvent> publish(OrderEvent event) {
        String key = event.orderId().toString();
        return kafkaTemplate.send(topicName, key, event)
                .thenApply(sendResult -> event);
    }
}

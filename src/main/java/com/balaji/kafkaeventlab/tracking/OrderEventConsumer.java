package com.balaji.kafkaeventlab.tracking;

import com.balaji.kafkaeventlab.event.OrderEvent;
import com.balaji.kafkaeventlab.order.OrderProjection;
import com.balaji.kafkaeventlab.order.OrderProjectionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OrderEventConsumer {

    private final TrackedEventRepository trackedEventRepository;
    private final OrderProjectionRepository orderProjectionRepository;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(
            TrackedEventRepository trackedEventRepository,
            OrderProjectionRepository orderProjectionRepository,
            ObjectMapper objectMapper
    ) {
        this.trackedEventRepository = trackedEventRepository;
        this.orderProjectionRepository = orderProjectionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.order-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, OrderEvent> record) throws JsonProcessingException {
        OrderEvent event = record.value();
        trackedEventRepository.save(new TrackedEvent(
                event.eventId(),
                event.eventType(),
                event.orderId(),
                record.topic(),
                record.partition(),
                record.offset(),
                event.occurredAt(),
                Instant.now(),
                objectMapper.writeValueAsString(event)
        ));
        updateReadModel(event);
    }

    private void updateReadModel(OrderEvent event) {
        if ("ORDER_CREATED".equals(event.eventType())) {
            orderProjectionRepository.save(new OrderProjection(
                    event.orderId(),
                    event.customerId(),
                    event.itemSku(),
                    event.quantity(),
                    event.amount(),
                    event.status(),
                    event.occurredAt()
            ));
            return;
        }

        if ("ORDER_STATUS_CHANGED".equals(event.eventType())) {
            orderProjectionRepository.findById(event.orderId()).ifPresent(order -> {
                order.changeStatus(event.status(), event.occurredAt());
                orderProjectionRepository.save(order);
            });
        }
    }
}

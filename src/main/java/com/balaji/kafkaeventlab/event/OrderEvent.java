package com.balaji.kafkaeventlab.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        String customerId,
        String itemSku,
        Integer quantity,
        BigDecimal amount,
        String status,
        Instant occurredAt,
        Integer schemaVersion
) {
    public static OrderEvent orderCreated(CreateOrderCommand command) {
        return new OrderEvent(
                UUID.randomUUID(),
                "ORDER_CREATED",
                UUID.randomUUID(),
                command.customerId(),
                command.itemSku(),
                command.quantity(),
                command.amount(),
                "CREATED",
                Instant.now(),
                1
        );
    }

    public static OrderEvent statusChanged(UUID orderId, ChangeOrderStatusCommand command) {
        return new OrderEvent(
                UUID.randomUUID(),
                "ORDER_STATUS_CHANGED",
                orderId,
                null,
                null,
                null,
                null,
                command.status(),
                Instant.now(),
                1
        );
    }
}

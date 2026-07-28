package com.balaji.kafkaeventlab.api;

import com.balaji.kafkaeventlab.event.ChangeOrderStatusCommand;
import com.balaji.kafkaeventlab.event.CreateOrderCommand;
import com.balaji.kafkaeventlab.event.OrderEvent;
import com.balaji.kafkaeventlab.event.OrderEventPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
public class OrderCommandController {

    private final OrderEventPublisher publisher;

    public OrderCommandController(OrderEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<OrderEvent> createOrder(@Valid @RequestBody CreateOrderCommand command) {
        return publisher.publish(OrderEvent.orderCreated(command));
    }

    @PostMapping("/{orderId}/status")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<OrderEvent> changeStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody ChangeOrderStatusCommand command
    ) {
        return publisher.publish(OrderEvent.statusChanged(orderId, command));
    }
}

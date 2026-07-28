package com.balaji.kafkaeventlab;

import com.balaji.kafkaeventlab.event.CreateOrderCommand;
import com.balaji.kafkaeventlab.event.OrderEvent;
import com.balaji.kafkaeventlab.event.OrderEventPublisher;
import com.balaji.kafkaeventlab.order.OrderProjectionRepository;
import com.balaji.kafkaeventlab.tracking.TrackedEventRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = "orders.events.v1",
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:0",
                "port=0"
        }
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class KafkaEventLabApplicationTests {

    @Autowired
    private OrderEventPublisher publisher;

    @Autowired
    private TrackedEventRepository trackedEventRepository;

    @Autowired
    private OrderProjectionRepository orderProjectionRepository;

    @Test
    void publishesConsumesAndTracksOrderCreatedEvent() {
        OrderEvent event = OrderEvent.orderCreated(new CreateOrderCommand(
                "customer-101",
                "SKU-KAFKA-BOOK",
                1,
                new BigDecimal("799.00")
        ));

        publisher.publish(event).join();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertThat(trackedEventRepository.findById(event.eventId())).isPresent();
                    assertThat(orderProjectionRepository.findById(event.orderId())).isPresent();
                });
    }
}

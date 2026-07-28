# Kafka Event Lab

An intermediate-level Spring Boot + Kafka project for learning event-driven concepts practically.

The app models a simple order workflow:

1. A REST API receives a command such as "create order".
2. The app publishes an `OrderEvent` to Kafka topic `orders.events.v1`.
3. Kafka stores the event in a partitioned, durable log.
4. A Spring Kafka consumer reads the event.
5. The consumer tracks the consumed event in H2 and updates an order read model.
6. You inspect the event through Kafka UI, Kafka CLI, H2, and REST query APIs.

## Tech Stack

- Java 17
- Spring Boot 3.5.x
- Spring for Apache Kafka
- Apache Kafka 4 in KRaft mode
- Kafka UI
- H2 in-memory database
- Maven

## Quick Start

Start Kafka and Kafka UI:

```bash
docker compose up -d
```

If this fails with `Cannot connect to the Docker daemon`, start Docker Desktop first and rerun the command.

Run the Spring Boot app:

```bash
mvn spring-boot:run
```

Create an order event:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-101",
    "itemSku": "SKU-KAFKA-BOOK",
    "quantity": 1,
    "amount": 799.00
  }'
```

Change the order status. Use the `orderId` returned by the create call:

```bash
curl -X POST http://localhost:8080/api/orders/<orderId>/status \
  -H "Content-Type: application/json" \
  -d '{"status": "PAYMENT_CONFIRMED"}'
```

View events tracked by the consumer:

```bash
curl http://localhost:8080/api/tracked-events
```

View the order projection/read model:

```bash
curl http://localhost:8080/api/order-projections
```

Open Kafka UI:

```text
http://localhost:8085
```

Open H2 console:

```text
http://localhost:8080/h2-console
```

Use JDBC URL:

```text
jdbc:h2:mem:kafkaeventlab
```

Username:

```text
sa
```

Password: leave empty.

## Project Structure

```text
src/main/java/com/balaji/kafkaeventlab
├── api       REST controllers
├── config    Kafka topic config
├── event     commands, event record, producer
├── order     read model / projection
└── tracking  Kafka consumer and consumed-event tracker
```

## Learning Goal

This is intentionally not a huge enterprise project. It focuses on concepts you need for product/platform roles:

- topics
- partitions
- offsets
- consumer groups
- event keys
- JSON event payloads
- producer/consumer flow
- event tracking
- read models/projections
- API-triggered events
- basic test automation for Kafka flows

See:

- `docs/kafka-learning-guide.md`
- `docs/testing-guide.md`
- `docs/kafka-event-inspection.md`
- `docs/concepts-with-project-instances.md`

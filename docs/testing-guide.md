# Testing Guide

## Automated Test

Run:

```bash
mvn test
```

The test uses Spring Kafka's embedded Kafka broker.

It verifies:

1. An `OrderEvent` is published.
2. The Kafka listener consumes it.
3. The event is saved in `TrackedEvent`.
4. The order read model is saved in `OrderProjection`.

## Manual API Test

Start infrastructure:

```bash
docker compose up -d
```

Start app:

```bash
mvn spring-boot:run
```

Create event:

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

Check tracked events:

```bash
curl http://localhost:8080/api/tracked-events
```

Check projection:

```bash
curl http://localhost:8080/api/order-projections
```

## Suggested Test Scenarios

### Scenario 1: Event Is Produced

Create an order and check Kafka UI.

Expected:

- topic has one message
- key is the generated `orderId`
- value contains `eventType: ORDER_CREATED`

### Scenario 2: Consumer Tracks Event

Call:

```bash
curl http://localhost:8080/api/tracked-events
```

Expected:

- event exists
- topic name is `orders.events.v1`
- partition and offset are populated

### Scenario 3: Projection Updates

Create order, copy `orderId`, then change status:

```bash
curl -X POST http://localhost:8080/api/orders/<orderId>/status \
  -H "Content-Type: application/json" \
  -d '{"status": "PAYMENT_CONFIRMED"}'
```

Expected:

- Kafka UI shows a second event
- `/api/order-projections` shows updated status

### Scenario 4: Consumer Catch-up

1. Stop Spring Boot app.
2. Keep Kafka running.
3. Start Spring Boot app again.

Expected:

- consumer resumes from committed offset
- no duplicate processing for already committed records

To force re-processing for learning, change:

```yaml
spring.kafka.consumer.group-id
```

to a new value in `application.yml`.

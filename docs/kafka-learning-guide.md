# Kafka Learning Guide

## 1. Mental Model

Kafka is an event streaming platform.

Think of it as a durable event log:

- A producer writes events to a topic.
- A topic is split into partitions.
- Each event gets an offset inside a partition.
- Consumers read events from partitions.
- Consumer groups coordinate who reads what.

In this project:

| Kafka Concept | Project Example |
|---|---|
| Producer | `OrderEventPublisher` |
| Topic | `orders.events.v1` |
| Event key | `orderId` |
| Event payload | `OrderEvent` JSON |
| Consumer | `OrderEventConsumer` |
| Consumer group | `order-tracker-service` |
| Offset | Stored in `TrackedEvent.offsetNumber` |
| Read model | `OrderProjection` |

## 2. Why Use `orderId` As Kafka Key?

Kafka sends records with the same key to the same partition.

Using `orderId` as the key helps preserve ordering for events of the same order:

```text
ORDER_CREATED(order-1)
ORDER_STATUS_CHANGED(order-1)
ORDER_STATUS_CHANGED(order-1)
```

Those should be processed in order for the same order.

## 3. Topic, Partition, Offset

Topic:

```text
orders.events.v1
```

Partitions:

```text
0, 1, 2
```

Offsets:

```text
partition 0: offset 0, 1, 2...
partition 1: offset 0, 1, 2...
partition 2: offset 0, 1, 2...
```

Offsets are per partition, not global across the whole topic.

## 4. Event Tracking

Kafka stores events in its own log. Your application usually stores a business-friendly view separately.

This project stores consumed metadata in H2:

```text
TrackedEvent
eventId
eventType
orderId
topicName
partitionNumber
offsetNumber
occurredAt
consumedAt
eventJson
```

This helps you learn:

- Did my consumer receive the event?
- Which partition did it land in?
- What offset did Kafka assign?
- What did the actual payload look like?

## 5. Read Model

`OrderProjection` is a simple read model.

When the consumer receives `ORDER_CREATED`, it creates an order projection.

When it receives `ORDER_STATUS_CHANGED`, it updates the projection status.

This is the basic idea behind event-driven read models.

## 6. Things To Experiment With

1. Create 5 orders and check which partitions they go to.
2. Change one order status multiple times and observe offsets.
3. Stop the Spring Boot app, create events with Kafka CLI, restart the app, and see if the consumer catches up.
4. Change the consumer group id and observe that events are re-consumed from earliest.
5. Change topic partitions from 3 to 1 and observe ordering behavior.

## 7. Not Covered Yet

This project avoids advanced complexity intentionally:

- schema registry
- Avro/Protobuf
- exactly-once processing
- retries and dead-letter topics
- Kafka Streams
- multi-broker clusters
- authentication/security
- production monitoring

Once comfortable, those are good next steps.

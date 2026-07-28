# Concepts With Project Instances

Use this as the quick revision sheet while reading the code.

## Producer

Class:

```text
OrderEventPublisher
```

What it does:

```text
Sends OrderEvent messages to Kafka using KafkaTemplate.
```

Key concept:

```text
Producer = application component that writes records to Kafka.
```

## Topic

Configured in:

```text
application.yml -> app.kafka.order-events-topic
KafkaTopicConfig
```

Value:

```text
orders.events.v1
```

Key concept:

```text
Topic = named stream/log where related events are stored.
```

## Event Key

Code:

```text
String key = event.orderId().toString();
```

Why:

```text
Same orderId should go to the same partition, preserving event order for one order.
```

## Event Payload

Class:

```text
OrderEvent
```

Example event types:

```text
ORDER_CREATED
ORDER_STATUS_CHANGED
```

Key concept:

```text
Payload = business event data serialized as JSON.
```

## Consumer

Class:

```text
OrderEventConsumer
```

Annotation:

```text
@KafkaListener
```

What it does:

```text
Receives events from Kafka, tracks metadata, and updates the order projection.
```

## Consumer Group

Configured in:

```text
spring.kafka.consumer.group-id: order-tracker-service
```

Key concept:

```text
Consumer group = logical app instance group that shares partitions.
```

With 3 partitions and 1 consumer:

```text
consumer-1 handles partitions 0, 1, 2
```

With 3 partitions and 3 consumers in the same group:

```text
consumer-1 handles partition 0
consumer-2 handles partition 1
consumer-3 handles partition 2
```

## Offset

Stored in:

```text
TrackedEvent.offsetNumber
```

Key concept:

```text
Offset = position of a record inside one partition.
```

## Partition

Configured in:

```text
KafkaTopicConfig -> partitions(3)
```

Stored in:

```text
TrackedEvent.partitionNumber
```

Key concept:

```text
Partition = ordered sub-log inside a topic.
```

## Event Tracking Table

Entity:

```text
TrackedEvent
```

Why it exists:

```text
Kafka stores the event. The app stores a learning-friendly audit record of what it consumed.
```

## Read Model / Projection

Entity:

```text
OrderProjection
```

Why it exists:

```text
APIs and UIs usually need a current-state view, not raw events only.
```

## Manual Flow

```text
POST /api/orders
  -> OrderCommandController
  -> OrderEventPublisher
  -> Kafka topic orders.events.v1
  -> OrderEventConsumer
  -> TrackedEvent + OrderProjection
  -> GET /api/tracked-events
  -> GET /api/order-projections
```

## What To Explain In Interviews

You can say:

```text
I built a Spring Boot Kafka event lab where REST commands publish domain events to Kafka. 
Events are keyed by aggregate id for partition-level ordering, consumed by a listener group, 
tracked with topic/partition/offset metadata, and projected into a read model for API queries.
```

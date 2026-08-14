# Kafka Learning Guide

## Project Mapping

| Kafka concept | Project instance |
| --- | --- |
| Broker | local Kafka server from `docker-compose.yml` or Testcontainers |
| Kafka admin | Spring Kafka `KafkaAdmin` plus `NewTopic` bean |
| Producer | `IdentityEventPublisher` |
| Topic | `identity.events.v1` |
| Event key | aggregate id, such as user id or access grant id |
| Event payload | `IdentityEvent` JSON |
| Listener / consumer | `IdentityEventConsumer` with `@KafkaListener` |
| Consumer group | `identity-platform-read-model` |
| Tracking table | `IdentityTrackedEvent` |
| Read model | `UserAccessSummaryProjection` |

## Broker

![Kafka broker flow](assets/kafka-broker-flow.svg)

A broker is the Kafka server that stores records and serves them to consumers.

The broker's job is infrastructure work:

- receive records from producers
- store records in topic partitions
- assign offsets
- serve records to consumers
- manage consumer group offset commits

The broker does not know identity business rules. It only knows records, topics, partitions, offsets, and consumer groups.

## Kafka Admin

![Kafka admin versus Kafka listener](assets/kafka-admin-vs-listener.svg)

Kafka admin is used to manage Kafka resources.

In this app, `KafkaTopicConfig` declares:

```java
NewTopic identityEventsTopic(...)
```

Spring Kafka uses admin functionality at startup to create or validate the topic.

Kafka admin is not the same as a Kafka listener. Admin manages topics and metadata. A listener consumes records.

## Kafka Listener

A Kafka listener is application code that runs when a record is consumed.

In this app, `IdentityEventConsumer` is the listener:

```java
@KafkaListener(topics = "${app.kafka.identity-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
```

The listener receives an `IdentityEvent`, tracks the event metadata, and updates read projections.

## Why Use Aggregate Id As Kafka Key?

Kafka preserves order within a partition. Using the aggregate id as the key keeps related lifecycle events for the same aggregate on the same partition.

Examples:

```text
USER_REGISTERED(user-1)
USER_STATUS_CHANGED(user-1)
```

For access grants:

```text
APPLICATION_ACCESS_GRANTED(access-1)
APPLICATION_ACCESS_REVOKED(access-1)
```

## Event Shape

`IdentityEvent` carries:

- `eventId`
- `eventType`
- `aggregateType`
- `aggregateId`
- `subjectUserId`
- `correlationId`
- `occurredAt`
- `schemaVersion`
- `data`

That gives consumers enough metadata to trace, route, validate, and evolve events.

## Domain Model To Domain Event Flow

![Domain model to domain event flow](assets/domain-event-flow.svg)

The domain model is the business model of the system: users, credentials, applications, access grants, and their allowed state changes.

When a business command succeeds, the app emits a domain event.

Example:

```text
GrantApplicationAccessCommand
  -> IdentityCommandService validates UserProfile and IdentityApplication
  -> ApplicationAccessGrant is saved
  -> IdentityEvent APPLICATION_ACCESS_GRANTED is created
  -> event is published to identity.events.v1
```

Important event metadata:

| Metadata | Why it exists |
| --- | --- |
| `eventType` | tells consumers exactly what happened |
| `aggregateId` | identifies the changed business object and becomes the Kafka key |
| `schemaVersion` | lets event contracts evolve safely |
| `correlationId` | lets logs/events/actions be traced as one workflow |

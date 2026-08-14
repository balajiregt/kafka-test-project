# Concepts With Project Instances

## Command API

Example:

```text
POST /api/identity/users
```

The controller validates the request and delegates to `IdentityCommandService`.

## Domain Model

![Domain model to domain event flow](assets/domain-event-flow.svg)

A domain model is the code representation of the business concepts, rules, and lifecycle of the system.

In this project, the identity platform domain model includes:

- `UserProfile`
- `UserCredential`
- `IdentityApplication`
- `ApplicationAccessGrant`
- `UserStatus`
- `CredentialStatus`
- `AccessStatus`

These are not Kafka concepts. They are identity-platform concepts. Kafka is only the transport used to publish facts about changes in this model.

Example:

```text
UserProfile
  -> has email, display name, department, job title, status
  -> can move from PENDING_ACTIVATION to ACTIVE or SUSPENDED
```

The domain model answers: "What does this business system know and what rules does it enforce?"

## Durable Write Model

Examples:

- `UserProfile`
- `UserCredential`
- `IdentityApplication`
- `ApplicationAccessGrant`

These represent current operational records owned by the identity platform.

## Kafka Broker

![Kafka broker flow](assets/kafka-broker-flow.svg)

A Kafka broker is a Kafka server.

Its role is to receive, store, and serve records for Kafka topics. A Kafka cluster is made of one or more brokers. In local development, this project runs one broker through `docker-compose.yml`.

The broker does this work:

- accepts records from producers
- stores records in topic partitions
- assigns offsets inside each partition
- serves records to consumers
- tracks consumer group offset commits
- replicates partition data in multi-broker clusters
- coordinates with other brokers for partition leadership

In this project:

```text
IdentityEventPublisher
  -> sends IdentityEvent to Kafka broker
  -> broker stores it in identity.events.v1
  -> IdentityEventConsumer reads it from the broker
```

The broker is not business logic. It does not know what a user, credential, or access grant means. It only stores and delivers records.

## Kafka Topic And Partition

Topic:

```text
identity.events.v1
```

A topic is a named stream of records. Producers write to it; consumers read from it.

Partition:

```text
identity.events.v1 partition 0
identity.events.v1 partition 1
identity.events.v1 partition 2
```

A partition is an ordered log inside a topic. Ordering is guaranteed within one partition, not across the whole topic.

## Domain Event

Example:

```text
APPLICATION_ACCESS_GRANTED
```

The event says something meaningful happened. It is not just a copy of an API response.

A domain event is a past-tense business fact emitted by the domain model.

Examples:

- `USER_REGISTERED`
- `USER_STATUS_CHANGED`
- `CREDENTIAL_REGISTERED`
- `APPLICATION_CREATED`
- `APPLICATION_ACCESS_GRANTED`
- `APPLICATION_ACCESS_REVOKED`

The event should be explicit enough that another service can understand what happened without knowing which API endpoint caused it.

## Identity Event Metadata

Each `IdentityEvent` carries both business data and technical metadata.

| Field | Meaning | Example |
| --- | --- | --- |
| `eventId` | Unique id for this event instance | `46d7f58f-...` |
| `eventType` | Business fact that happened | `APPLICATION_ACCESS_GRANTED` |
| `aggregateType` | Kind of business object changed | `ACCESS_GRANT` |
| `aggregateId` | Id of the business object changed | access grant id |
| `subjectUserId` | User affected by the event | user id |
| `correlationId` | Trace id connecting related work | request or workflow id |
| `occurredAt` | Business time of the event | timestamp |
| `schemaVersion` | Payload contract version | `1` |
| `data` | Event-specific business payload | role/application/user details |

## Aggregate And Aggregate Id

An aggregate is a business object whose changes should be treated as one consistency boundary.

In this project:

| Aggregate | Aggregate id |
| --- | --- |
| user profile | `userId` |
| credential | `credentialId` |
| application | `applicationId` |
| access grant | `accessId` |

The aggregate id is used as the Kafka record key. That keeps lifecycle events for the same aggregate on the same Kafka partition.

Example:

```text
APPLICATION_ACCESS_GRANTED(access-1)
APPLICATION_ACCESS_REVOKED(access-1)
```

Both records use `access-1` as key, so Kafka routes them to the same partition and preserves their order.

## Schema Version

`schemaVersion` identifies the shape of the event payload.

Example:

```text
schemaVersion = 1
```

If a future version adds fields, renames fields, or changes payload structure, producers can emit `schemaVersion = 2`. Consumers can then decide how to handle old and new versions safely.

Schema versioning is what lets enterprise event contracts evolve without breaking every consumer at once.

## Correlation Id

`correlationId` is a trace id for connecting related operations across logs, events, and services.

Example flow:

```text
HTTP request starts with correlationId abc-123
  -> user is activated
  -> USER_STATUS_CHANGED is published with correlationId abc-123
  -> consumer logs projection update with correlationId abc-123
```

When a production issue happens, correlation id helps you ask: "Which API call, event, consumer action, and database update belonged to the same workflow?"

## Kafka Topic

```text
identity.events.v1
```

This topic carries identity lifecycle events. The version suffix lets consumers migrate safely when event contracts evolve.

## Kafka Admin

![Kafka admin versus Kafka listener](assets/kafka-admin-vs-listener.svg)

Kafka admin means the administrative client/functionality used to manage broker resources.

In Spring Boot, Kafka admin is usually represented by Spring's `KafkaAdmin` auto-configuration plus `NewTopic` beans.

In this project:

```java
@Bean
NewTopic identityEventsTopic(...)
```

That bean tells Spring Kafka to ask the broker to create or validate the `identity.events.v1` topic when the application starts.

Kafka admin is used for infrastructure-level work:

- create topics
- describe topics
- change topic configuration
- check cluster metadata
- inspect consumer groups

Kafka admin is not used to process business events. It prepares and manages Kafka resources.

## Kafka Listener

A Kafka listener is application code that subscribes to a topic and runs when records arrive.

In this project:

```java
@KafkaListener(topics = "${app.kafka.identity-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
```

This annotation makes `IdentityEventConsumer.consume(...)` a Kafka listener.

Its work:

- poll records from the broker
- deserialize the record value into `IdentityEvent`
- receive metadata such as topic, partition, and offset
- run business-side consumption logic
- commit offsets according to the configured listener container behavior

In our app, the listener:

```text
reads IdentityEvent
  -> saves IdentityTrackedEvent
  -> updates UserAccessSummaryProjection when relevant
```

## Consumer Group

```text
identity-platform-read-model
```

Consumers in the same group divide partitions among themselves.

## Event Tracking

`IdentityTrackedEvent` stores:

- event id
- event type
- aggregate type and id
- subject user id
- correlation id
- topic
- partition
- offset
- payload JSON

That gives you auditability and makes test assertions precise.

## Projection

`UserAccessSummaryProjection` is a read-optimized view of user-to-application access.

It is updated asynchronously from events, so tests should wait for eventual consistency instead of assuming immediate visibility.

## Full Business Operation To Event Flow

![Domain event flow](assets/domain-event-flow.svg)

Example: grant application access.

```text
POST /api/identity/access-grants
  -> IdentityCommandController
  -> IdentityCommandService
  -> validates user and application exist
  -> saves ApplicationAccessGrant
  -> creates IdentityEvent with:
       eventType = APPLICATION_ACCESS_GRANTED
       aggregateType = ACCESS_GRANT
       aggregateId = accessId
       subjectUserId = userId
       schemaVersion = 1
       correlationId = generated trace id
  -> IdentityEventPublisher sends event to Kafka
  -> Kafka broker stores event in identity.events.v1
  -> IdentityEventConsumer receives event
  -> IdentityTrackedEvent stores audit metadata
  -> UserAccessSummaryProjection is updated for query APIs
```

This is the key separation:

- command API handles the user request
- domain model enforces business state
- domain event announces what happened
- Kafka broker stores and delivers the event
- listener consumes the event
- projection supports read/query use cases

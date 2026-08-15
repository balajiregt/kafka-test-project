# Testing Kafka-Driven Account and Authentication Flows Without Turning E2E Tests Into a Bottleneck

Account and authentication services often look simple from the outside: create a profile, activate the user, register credentials, sign in, reset a password.

Inside the platform, those operations usually create a trail of business facts:

- `USER_REGISTERED`
- `USER_STATUS_CHANGED`
- `CREDENTIAL_REGISTERED`
- `SIGN_IN_SUCCEEDED`
- `SIGN_IN_FAILED`
- `PASSWORD_RESET_REQUESTED`
- `PASSWORD_RESET_COMPLETED`

In an event-driven architecture, those facts are published as domain events. Kafka is the event transport. The application owns the business rules.

The important testing question is not "Can I assert Kafka messages from a browser test?"

The better question is:

> Which behavior should be tested at which layer so the suite remains trustworthy, fast, and scalable?

## The Sample Flow

This project models a focused account lifecycle:

```text
Register user
  -> activate user
  -> register credential reference
  -> sign in
  -> request password reset
  -> complete password reset
```

Each successful command persists operational state in H2 and publishes an `AccountEvent` to:

```text
account.events.v1
```

The Kafka listener consumes the event and stores an audit-friendly tracking record:

```text
EventEvidence
  eventId
  eventType
  aggregateType
  aggregateId
  subjectUserId
  correlationId
  topicName
  partitionNumber
  offsetNumber
  eventJson
```

That tracking record makes asynchronous behavior observable without pretending Kafka is a synchronous API.

## The Unique Layer: Event Intelligence

The project goes one step beyond "assert that Kafka received a message."

It builds an Event Assurance Graph from consumed events:

```text
User
  <- AFFECTS_SUBJECT - Event
  <- AUTHENTICATES_WITH - Sign-in attempt
  <- RESETS_CREDENTIAL - Password reset request
```

The graph is computed from the normal tracking table. There is no Neo4j dependency in the default flow.

On top of that graph, a deterministic advisor evaluates questions like:

- Was the user registered before activation?
- Was a credential registered before successful sign-in?
- Was password reset requested before it was completed?
- Do consumed events contain required metadata such as aggregate id, correlation id, schema version, and offset?
- Should this behavior be tested at API, Kafka, contract/schema, or UI/e2e level?

That is the useful version of "agentic" for this project. The advisor reasons over evidence and gives testing guidance, but it does not require a live LLM call. This keeps CI stable and makes the feature practical rather than decorative.

Example endpoints:

```text
GET /api/assurance/subjects/{subjectId}/journey
GET /api/assurance/subjects/{subjectId}/findings
GET /api/assurance/testing-recommendations
GET /assurance-report.html?subjectId={subjectId}
```

The JSON endpoints are useful for automated tests. The report page is useful for humans: summary cards, advisor bars, event timeline, graph edges, and testing recommendation panels all come from the same evidence.

The key idea is:

> Kafka validation is not just message assertion; it is journey assurance.

## The Core Design

The application separates four responsibilities:

```text
Command API
  -> validates HTTP request
  -> calls application service

Domain model
  -> owns profile, credential, sign-in, reset-request state
  -> enforces business transitions

Domain event
  -> announces the past-tense business fact
  -> carries event metadata

Kafka listener
  -> consumes event
  -> stores consumed metadata for audit/test verification
```

This separation keeps Kafka from leaking into every test and keeps UI/API tests from becoming message-broker tests.

## Essential Kafka Concepts For This Project

**Broker**

A Kafka broker is the Kafka server. It receives records from producers, stores them in topic partitions, assigns offsets, and serves records to consumers. It does not understand account and authentication business rules.

**Topic**

A topic is a named stream of records. This project uses:

```text
account.events.v1
```

**Partition**

A partition is an ordered log inside a topic. Kafka guarantees ordering within a partition, not across the entire topic.

**Offset**

An offset is the position of a record inside a partition. Offsets are useful for auditing and debugging consumer progress.

**Producer**

The producer writes events to Kafka. In this project, `AccountEventPublisher` sends `AccountEvent` records.

**Listener / consumer**

The listener reads records from Kafka. In this project, `AccountEventConsumer` uses `@KafkaListener`.

**Consumer group**

A consumer group coordinates which consumer instance reads which partitions. This project uses:

```text
account-event-tracker
```

**Kafka admin**

Kafka admin functionality manages infrastructure resources such as topics. In Spring Boot, a `NewTopic` bean lets the app create or validate a topic at startup.

## Domain Event Metadata

Each event has two kinds of information.

Business data:

```text
email
displayName
credentialType
reset status
sign-in result
```

Technical metadata:

| Field | Purpose |
| --- | --- |
| `eventId` | unique id for the event instance |
| `eventType` | tells consumers what happened |
| `aggregateType` | type of business object changed |
| `aggregateId` | id of that business object; also used as Kafka key |
| `subjectUserId` | user affected by the event |
| `correlationId` | connects logs, events, and workflow steps |
| `occurredAt` | time the business event occurred |
| `schemaVersion` | allows event contract evolution |

That metadata is what makes the system testable and operable.

## What To Test At Each Layer

### 1. Unit Tests

Use unit tests for small business rules and event construction.

Test examples:

- user starts in `PENDING_ACTIVATION`
- user status can change to `ACTIVE`
- reset request starts as `REQUESTED`
- completing reset changes status to `COMPLETED`
- event factory creates the correct `eventType`, `aggregateType`, and `schemaVersion`

Do not start Kafka here.

### 2. API Integration Tests

API integration tests should prove request/response behavior and synchronous application state.

In this project, `AccountApiIntegrationTests` starts Spring Boot with MockMvc and JPA, but mocks the Kafka publisher.

Test examples:

- `POST /api/accounts/users` validates request body
- user registration persists a `UserProfile`
- activation changes `UserStatus`
- credential registration persists a `UserCredential`
- sign-in stores a `SignInAttempt`
- password reset request persists a `PasswordResetRequest`
- API returns stable event-shaped JSON
- invalid email returns `400`

Kafka should usually be mocked at this layer. The goal is not to prove broker behavior.

### 3. Kafka Integration Tests

Kafka integration tests should prove real broker behavior.

In this project, `AccountKafkaIntegrationTests` uses Testcontainers Kafka.

Test examples:

- Spring Boot connects to a real Kafka broker
- topic creation works
- producer serializes `AccountEvent`
- event key is the aggregate id
- headers include `eventType`, `correlationId`, `schemaVersion`
- listener consumes from the configured topic
- consumer stores topic, partition, and offset
- asynchronous assertions wait with Awaitility
- the assurance graph can see events that passed through the real producer and listener
- the advisor returns passing lifecycle and metadata findings for the happy path

This is the right place for Kafka correctness.

### 4. End-To-End Tests

E2E tests should be thin.

A browser or black-box test can verify one critical business journey:

```text
User completes password reset
  -> user can sign in with the new credential
```

Avoid putting detailed Kafka assertions in every E2E spec. Once UI, backend, broker timing, consumer groups, and database assertions all live in the same test, failures become harder to diagnose and CI becomes slower.

## What Not To Test In E2E

Avoid using Playwright or browser tests as the primary Kafka validation layer.

Do not use E2E specs to exhaustively verify:

- every Kafka header
- every event schema field
- topic partition behavior
- retry/DLQ logic
- consumer offset handling
- every producer/consumer scenario

Those belong in lower-level Kafka integration tests with Testcontainers.

The assurance layer makes this separation easier to explain. UI tests can stay focused on the visible journey. Kafka tests can prove event transport and listener behavior. The advisor can then summarize the event journey and point each finding to the correct testing layer.

## How To Scale The Test Suite

A scalable test strategy looks like this:

```text
Many unit tests
  -> fast business-rule confidence

Many API integration tests
  -> HTTP/service/database confidence without Kafka timing

Focused Kafka integration tests
  -> real broker confidence for event contracts and consumers

Few E2E tests
  -> critical user journey confidence
```

For CI stability:

- use unique test data per test
- use unique consumer groups where needed
- avoid shared topic cleanup assumptions
- wait for eventual consistency with Awaitility
- keep Kafka tests independent from browser tests
- run Kafka/Testcontainers tests only where Docker is available
- keep E2E tests small and business-focused

## The Main Lesson

Kafka testing should be layered.

The API layer proves the account/profile authentication use case behaves correctly.

The Kafka integration layer proves the event transport, serialization, listener, and metadata tracking work correctly.

The E2E layer proves the user journey still works.

When each layer has a clear purpose, the test suite becomes easier to scale, easier to debug, and much less flaky.

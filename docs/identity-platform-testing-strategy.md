# Identity Platform Testing Strategy

This project intentionally separates Kafka tests from API integration tests.

## What Kafka Tests Should Cover

Kafka tests prove event contracts, broker integration, and asynchronous consumption.
They should use Testcontainers because the behavior depends on a real broker.

Cover these at Kafka integration level:

| Concern | Example in identity platform |
| --- | --- |
| Topic provisioning | `identity.events.v1` exists with expected partitions |
| Producer routing | event key is the aggregate id |
| Producer headers | `eventType`, `correlationId`, `schemaVersion` are present |
| Payload contract | `IdentityEvent` has event id, type, aggregate, subject user, timestamp, schema version, data |
| Serialization | Spring Kafka JSON serializer/deserializer handles the event |
| Consumer subscription | listener consumes from configured topic and group |
| Offset/metadata capture | tracked event stores topic, partition, offset |
| Projection behavior | access grant event creates `UserAccessSummaryProjection` |
| Eventual consistency | API write is not assumed visible in read model until consumer processes event |
| Ordering assumptions | only assert order for records with the same key/partition |
| Failure policies | retries, poison messages, DLQ, idempotency, duplicate handling |

Keep Kafka tests focused. Do not click through UI flows. Do not retest every controller validation rule here.

## What API Integration Tests Should Cover

API integration tests prove request/response behavior and synchronous application state.
Kafka should usually be mocked or replaced with a test double at this level.

Cover these at API integration level:

| Concern | Example in identity platform |
| --- | --- |
| HTTP routing | `POST /api/identity/users` reaches the command handler |
| Request validation | invalid email or missing role returns `400` |
| Response shape | accepted command returns event metadata |
| Service orchestration | user registration persists a `UserProfile` |
| Database constraints | duplicate user email is rejected |
| Authorization policy | caller can or cannot grant access, once security is added |
| Error mapping | domain validation maps to stable API errors |
| Query APIs | users, applications, summaries, and tracked events are returned correctly |

Do not require a Kafka broker for most API tests. If API tests depend on Kafka timing, they become slower and harder to parallelize.

## Recommended Test Pyramid

1. **Unit tests**
   Event factory logic, status-transition rules, validators, mappers.

2. **API integration tests**
   Spring MVC + service + JPA. Mock Kafka publishing.

3. **Kafka integration tests**
   Spring Boot + Testcontainers Kafka + real producer and consumer.

4. **End-to-end tests**
   A tiny number of browser or black-box flows. Assert user-visible outcomes first. Kafka assertions here should be rare smoke checks using correlation ids.

## Why Not Put Kafka Assertions In E2E Specs?

Combining UI flow plus Kafka validation in the same spec couples unrelated failure modes:

- browser selector changed
- backend validation changed
- Kafka broker was slow
- consumer group had stale offsets
- test data collided with another run

That combination tends to reduce parallelism and increase flaky CI failures. Kafka correctness should be proven below E2E, where setup and assertions are deterministic.

## Current Tests In This Repo

| Test | Level | Kafka behavior |
| --- | --- | --- |
| `IdentityApiIntegrationTests` | API integration | mocked publisher |
| `IdentityPlatformKafkaIntegrationTests` | Kafka integration | Testcontainers Kafka |

This is the scalable baseline: API tests stay fast and deterministic, while Kafka behavior is validated with a real broker at the proper layer.

# Account Profile and Authentication Event Lab

An enterprise-shaped Spring Boot + Kafka sample for account profile and authentication workflows.

The app models a small account profile and authentication service:

![Account event flow](docs/assets/domain-event-flow.svg)

1. REST command APIs manage user profiles, credentials, sign-in attempts, password reset requests, and user status.
2. Each command persists the operational record in H2.
3. The command emits an `AccountEvent` to Kafka topic `account.events.v1`.
4. A Spring Kafka consumer tracks consumed events with topic, partition, offset, correlation id, and payload JSON.
5. Tests separate API integration concerns from Kafka/event-flow concerns.

## Tech Stack

- Java 17
- Spring Boot 3.5.x
- Spring Web, Validation, Data JPA, Actuator
- Spring for Apache Kafka
- Apache Kafka 4 in KRaft mode for local development
- Testcontainers Kafka for CI-style Kafka tests
- H2 in-memory database
- Maven

## Quick Start

Start Kafka and Kafka UI:

```bash
docker compose up -d
```

Run the app:

```bash
mvn spring-boot:run
```

Register a user:

```bash
curl -X POST http://localhost:8080/api/accounts/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "anika.rao@example.com",
    "displayName": "Anika Rao",
    "department": "Finance",
    "jobTitle": "Controller"
  }'
```

Activate the user:

```bash
curl -X POST http://localhost:8080/api/accounts/users/<userId>/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "ACTIVE",
    "reason": "HR onboarding completed"
  }'
```

Register a credential reference:

```bash
curl -X POST http://localhost:8080/api/accounts/credentials \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "<userId>",
    "credentialType": "PASSWORD_HASH",
    "credentialReference": "password-hash:v1:anika",
    "mfaEnabled": true
  }'
```

Sign in:

```bash
curl -X POST http://localhost:8080/api/accounts/sign-ins \
  -H "Content-Type: application/json" \
  -d '{
    "email": "anika.rao@example.com",
    "credentialReference": "password-hash:v1:anika"
  }'
```

Request password reset:

```bash
curl -X POST http://localhost:8080/api/accounts/password-reset-requests \
  -H "Content-Type: application/json" \
  -d '{"email": "anika.rao@example.com"}'
```

Inspect read-side data:

```bash
curl http://localhost:8080/api/accounts/users
curl http://localhost:8080/api/accounts/users/<userId>/credentials
curl http://localhost:8080/api/accounts/users/<userId>/sign-in-attempts
curl http://localhost:8080/api/accounts/users/<userId>/password-reset-requests
curl http://localhost:8080/api/accounts/tracked-events
```

Kafka UI:

```text
http://localhost:8085
```

H2 console:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:kafkaeventlab
```

## Project Structure

```text
src/main/java/com/balaji/kafkaeventlab
├── config
│   └── KafkaTopicConfig
└── account
    ├── api          command/query controllers and API errors
    ├── application  command DTOs and use-case service
    ├── domain       durable account/profile/auth records and repositories
    ├── event        AccountEvent and Kafka publisher
    └── tracking     Kafka consumer and consumed-event audit log
```

## Test Layout

- `AccountApiIntegrationTests`: API + validation + service + JPA. Kafka publisher is mocked because broker behavior is not the point of this level.
- `AccountKafkaIntegrationTests`: Spring Boot + Testcontainers Kafka + real producer/consumer + consumed-event tracking assertions.

See [docs/account-testing-strategy.md](docs/account-testing-strategy.md).

For a blog-style write-up, see [docs/medium-account-kafka-testing.md](docs/medium-account-kafka-testing.md).

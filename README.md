# Identity Event Lab

An enterprise-shaped Spring Boot + Kafka sample for identity-platform workflows.

The app models a small identity platform:

![Identity domain event flow](docs/assets/domain-event-flow.svg)

1. REST command APIs manage user profiles, credentials, applications, and access grants.
2. Each command persists the operational record in H2.
3. The command emits an `IdentityEvent` to Kafka topic `identity.events.v1`.
4. A Spring Kafka consumer tracks consumed events with topic, partition, offset, correlation id, and payload JSON.
5. The consumer maintains a query-friendly user access summary projection.
6. Tests separate API integration concerns from Kafka/event-flow concerns.

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
curl -X POST http://localhost:8080/api/identity/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "anika.rao@example.com",
    "displayName": "Anika Rao",
    "department": "Finance",
    "jobTitle": "Controller"
  }'
```

Create an application:

```bash
curl -X POST http://localhost:8080/api/identity/applications \
  -H "Content-Type: application/json" \
  -d '{
    "applicationCode": "ERP",
    "displayName": "Enterprise Resource Planning",
    "ownerTeam": "enterprise-platform",
    "defaultRoles": ["VIEWER", "APPROVER"]
  }'
```

Grant application access:

```bash
curl -X POST http://localhost:8080/api/identity/access-grants \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "REPLACE_WITH_USER_ID",
    "applicationId": "REPLACE_WITH_APPLICATION_ID",
    "roleName": "APPROVER",
    "grantedBy": "iam-admin"
  }'
```

Inspect read-side data:

```bash
curl http://localhost:8080/api/identity/users
curl http://localhost:8080/api/identity/applications
curl http://localhost:8080/api/identity/access-summaries
curl http://localhost:8080/api/identity/tracked-events
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
└── identity
    ├── api          command/query controllers and API errors
    ├── application  command DTOs and use-case service
    ├── domain       durable identity records and repositories
    ├── event        IdentityEvent and Kafka publisher
    ├── projection   read-side user access summary
    └── tracking     Kafka consumer and consumed-event audit log
```

## Test Layout

- `IdentityApiIntegrationTests`: API + validation + service + JPA. Kafka publisher is mocked because broker behavior is not the point of this level.
- `IdentityPlatformKafkaIntegrationTests`: Spring Boot + Testcontainers Kafka + real producer/consumer + read-side projection assertions.

See [docs/identity-platform-testing-strategy.md](docs/identity-platform-testing-strategy.md).

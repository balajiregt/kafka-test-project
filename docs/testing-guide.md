# Testing Guide

Run all tests:

```bash
mvn test
```

Kafka integration tests require Docker because they use Testcontainers Kafka.

## API Integration Test

`IdentityApiIntegrationTests` starts Spring Boot with MockMvc and JPA, but mocks `IdentityEventPublisher`.

Use this style for:

- request validation
- HTTP status codes
- response JSON
- service orchestration
- database writes
- API error mapping

## Kafka Integration Test

`IdentityPlatformKafkaIntegrationTests` starts Spring Boot with a real Kafka broker from Testcontainers.

Use this style for:

- producer serialization
- topic connection
- event headers
- event tracking
- consumer processing
- read-side projection updates
- eventual consistency assertions

## Manual Smoke Flow

1. Start Kafka:

   ```bash
   docker compose up -d
   ```

2. Run the app:

   ```bash
   mvn spring-boot:run
   ```

3. Use `requests.http` or curl commands from the README.

4. Open Kafka UI at:

   ```text
   http://localhost:8085
   ```

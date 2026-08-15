# Testing Guide

Run all tests:

```bash
mvn test
```

Kafka integration tests require Docker because they use Testcontainers Kafka.

## API Integration Test

`AccountApiIntegrationTests` starts Spring Boot with MockMvc and JPA, but mocks `AccountEventPublisher`.
The test also stores synthetic consumed-event evidence so the assurance endpoints can be verified without a broker.

Use this style for:

- request validation
- HTTP status codes
- response JSON
- service orchestration
- database writes
- API error mapping
- profile/auth lifecycle behavior
- assurance journey and advisor API responses

## Advisor Unit Test

`EventAssuranceAdvisorTests` runs config-driven event-intelligence rules without Spring, Kafka, or a database.

Use this style for:

- lifecycle ordering rules
- failed sign-in classification
- password reset ordering
- Kafka metadata classification

## Kafka Integration Test

`AccountKafkaIntegrationTests` starts Spring Boot with a real Kafka broker from Testcontainers.

Use this style for:

- producer serialization
- topic connection
- event headers
- event tracking
- consumer processing
- event-driven read-side projection updates
- eventual consistency assertions
- assurance graph/advisor behavior over events that passed through Kafka

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

4. Inspect assurance output:

   ```text
   GET /api/assurance/subjects/{subjectId}/journey
   GET /api/assurance/subjects/{subjectId}/findings
   GET /api/assurance/testing-recommendations
   GET /assurance-report.html?subjectId={subjectId}
   ```

5. Open Kafka UI at:

   ```text
   http://localhost:8085
   ```

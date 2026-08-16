# Event Quality Testing Context

This file is the project knowledge pack for the Event Quality Testing Config Assistant. Keep it domain-neutral, safe to commit, and focused on event-driven QA/testing. Do not include secrets, live customer data, private credentials, or environment-specific tokens.

Use this Markdown to explain the application in human language. Use `app.assurance` YAML for executable rules.

## Application Summary

Describe the application boundary, the business outcomes it owns, and the main commands or workflows that should produce events.

Example:

- The application owns account profile and authentication lifecycle evidence.
- It emits Kafka events when account state changes.
- The quality testing framework checks whether emitted/consumed events prove the expected journey.

## Domain Vocabulary

Define the terms that matter for event rules.

- Subject: the business entity or person/account the journey belongs to.
- Aggregate: the event stream owner, such as an account, credential, order, subscription, or payment.
- Command: the API or message-driven action that attempts to change state.
- Domain event: the immutable fact emitted after a meaningful business change.
- Correlation id: the trace value that links commands, events, logs, and downstream processing.

## Business Capabilities

List capabilities in a way the assistant can map to test layers.

- Capability name:
- Primary commands:
- Expected events:
- Important consumers:
- Failure policies:
- Recommended test emphasis:

## Core Lifecycles

Write event sequences that should hold true for a subject or aggregate.

```text
PREREQUISITE_EVENT -> RESULT_EVENT -> FOLLOW_UP_EVENT
```

For each lifecycle, capture:

- Required prior events.
- Events that indicate a warning or failed journey.
- Events that can arrive more than once.
- Events that may arrive asynchronously or out of order.

## Event Catalog

List the events that matter to assurance.

| Event Type | Aggregate Type | Subject Field | Key Metadata | Notes |
| --- | --- | --- | --- | --- |
| REPLACE_WITH_EVENT | REPLACE_WITH_AGGREGATE | subjectId | correlationId, schemaVersion | Replace with project details. |

## Aggregate And Subject Mapping

Explain how the framework should connect events into a graph.

- Which field identifies the subject?
- Which field identifies the aggregate?
- Which event types represent credentials, sessions, reset requests, subscriptions, payments, orders, or other linked entities?
- Which identifiers should become graph node ids?

## Producers And Consumers

Capture ownership and runtime behavior.

- Producer service:
- Topic:
- Consumer service:
- Consumer group:
- Retry policy:
- DLQ policy:
- Idempotency key:
- Ordering guarantee:

## Test-Layer Boundaries

Use this section to guide dynamic recommendations.

- API integration tests should prove command validation, persistence, and synchronous API behavior.
- Kafka integration tests should prove publication, topic routing, listener behavior, offsets, correlation ids, and consumed evidence.
- Contract/schema tests should prove event shape, required metadata, schema versioning, and compatibility.
- UI/e2e tests should prove only critical user journeys, not every Kafka assertion.

## Evidence Sources

Describe where the assurance framework should read evidence from.

- Local development:
- CI:
- Shared test environment:
- Kubernetes/log source:
- Trace or observability source:

## Known Risks And Open Questions

Use this section to keep assumptions visible.

- Unknown event ownership:
- Missing schema details:
- Unclear ordering guarantees:
- Missing consumer evidence:
- Areas intentionally out of scope:

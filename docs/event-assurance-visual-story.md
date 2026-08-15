# Account Event Assurance Visual Story

This project treats Kafka events as test evidence. The assurance layer converts that evidence into a journey graph, applies deterministic advisor rules, and shows precise metrics that help a developer decide whether the behavior is healthy before merge.

## Shift-Left Flow

```mermaid
flowchart LR
    A["Developer runs mvn test locally"] --> B["Account API executes use case"]
    B --> C["Domain state is persisted"]
    B --> D["Domain event is published"]
    D --> E["Kafka topic"]
    E --> F["Listener consumes event"]
    F --> G["Tracked event table"]
    G --> H["Assurance graph"]
    H --> I["Advisor findings"]
    I --> J{"Any FAIL finding?"}
    J -- "Yes" --> K["Fix before PR or merge"]
    J -- "No" --> L["Change is safer to review"]
```

## Evidence Graph

```mermaid
flowchart TD
    U["User node"]:::user
    UR["USER_REGISTERED event"]:::event
    US["USER_STATUS_CHANGED event"]:::event
    CR["CREDENTIAL_REGISTERED event"]:::event
    SI["SIGN_IN_SUCCEEDED event"]:::event
    PR["PASSWORD_RESET_REQUESTED event"]:::event
    PC["PASSWORD_RESET_COMPLETED event"]:::event
    C["Credential node"]:::credential
    S["Sign-in attempt node"]:::signin
    R["Password reset node"]:::reset

    U -. "AFFECTS_SUBJECT" .- UR
    U -. "AFFECTS_SUBJECT" .- US
    U -. "AFFECTS_SUBJECT" .- CR
    U -. "AFFECTS_SUBJECT" .- SI
    U -. "AFFECTS_SUBJECT" .- PR
    U -. "AFFECTS_SUBJECT" .- PC

    C -->|"PRODUCED_EVENT"| CR
    S -->|"PRODUCED_EVENT"| SI
    R -->|"PRODUCED_EVENT"| PR
    R -->|"PRODUCED_EVENT"| PC
    S -->|"AUTHENTICATES_WITH"| U
    R -->|"RESETS_CREDENTIAL"| U

    UR -->|"FOLLOWS_EVENT"| US
    US -->|"FOLLOWS_EVENT"| CR
    CR -->|"FOLLOWS_EVENT"| SI
    SI -->|"FOLLOWS_EVENT"| PR
    PR -->|"FOLLOWS_EVENT"| PC

    classDef user fill:#dbeafe,stroke:#2563eb,color:#17202a
    classDef event fill:#e5e7eb,stroke:#17202a,color:#17202a
    classDef credential fill:#ccfbf1,stroke:#0f766e,color:#17202a
    classDef signin fill:#ede9fe,stroke:#6d4aff,color:#17202a
    classDef reset fill:#fef3c7,stroke:#b76a00,color:#17202a
```

## What The Interactive Graph Should Answer

```mermaid
flowchart LR
    A["Click a node"] --> B["Highlight direct relationships"]
    B --> C["Show connected events and aggregates"]
    C --> D["Show Kafka metadata"]
    D --> E["Reveal data-point metrics"]
    E --> F["Support advisor finding triage"]
```

The report focuses on precise metrics instead of broad dashboard noise:

- `metadataCompleteEvents`: how many consumed events contain required event metadata.
- `missingMetadataEvents`: how many events would be unsafe for consumers or contract tests.
- `sequenceEdges`: how much lifecycle ordering evidence exists.
- `offsetSpan`: how far the consumed records span in Kafka offsets.
- `averageConsumeLagMillis`: average delay between event occurrence and listener tracking.
- `journeyDurationMillis`: duration from first observed event to last observed event.
- `mostConnectedNodeId` and `mostConnectedNodeDegree`: the graph hotspot for the journey.

## Scalable Testing Boundary

```mermaid
flowchart TD
    A["Behavior to validate"] --> B{"Best layer?"}
    B --> C["API integration"]
    B --> D["Kafka integration"]
    B --> E["Contract/schema"]
    B --> F["UI/e2e"]
    B --> G["Assurance graph"]

    C --> C1["HTTP validation, business decision, database state"]
    D --> D1["Producer, broker, listener, offsets, tracking"]
    E --> E1["Required metadata and payload compatibility"]
    F --> F1["Visible browser outcome only"]
    G --> G1["Journey correctness across event evidence"]
```

The intended direction is a dynamic framework: account authentication is the reference domain, but lifecycle rules, metadata expectations, edge mappings, and test-layer recommendations should eventually move into configuration such as `event-assurance.yml` or be derived from AsyncAPI.

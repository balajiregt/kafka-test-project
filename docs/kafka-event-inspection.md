# Kafka Event Inspection

Start local Kafka:

```bash
docker compose up -d
```

Open Kafka UI:

```text
http://localhost:8085
```

Navigate to:

```text
local-kafka -> Topics -> account.events.v1 -> Messages
```

Useful checks:

- key is the aggregate id
- headers include `eventType`, `correlationId`, and `schemaVersion`
- value has `AccountEvent` metadata and business data
- records for the same key stay in the same partition

Kafka CLI from the broker container:

```bash
docker exec -it account-event-lab-broker \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic account.events.v1 \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "
```

Describe topic:

```bash
docker exec -it account-event-lab-broker \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic account.events.v1
```

Inspect events consumed by the app:

```bash
curl http://localhost:8080/api/accounts/tracked-events
```

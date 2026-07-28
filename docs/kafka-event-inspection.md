# How To View Kafka Events

Kafka does not store events like rows in a SQL table. It stores them in append-only log segments inside topic partitions.

For learning, inspect events through these four paths.

## Option 1: Kafka UI

Start Kafka:

```bash
docker compose up -d
```

Open:

```text
http://localhost:8085
```

Navigate:

```text
local-kafka -> Topics -> orders.events.v1 -> Messages
```

You should see:

- key: order id
- value: JSON event
- partition
- offset
- timestamp

## Option 2: Kafka Console Consumer

Read from the beginning:

```bash
docker exec -it kafka-event-lab-broker \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic orders.events.v1 \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" | "
```

Press `Ctrl+C` to stop.

## Option 3: Kafka Topic Description

Describe the topic:

```bash
docker exec -it kafka-event-lab-broker \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic orders.events.v1
```

This shows:

- partition count
- leader broker
- replicas
- ISR

## Option 4: Application Tracking API

The app consumer stores consumed metadata in H2.

```bash
curl http://localhost:8080/api/tracked-events
```

This is useful because it shows:

- topic
- partition
- offset
- consumed timestamp
- event payload

## Where Are Events Physically Stored?

Inside the Docker volume:

```text
kafka-data
```

Kafka writes binary log segments internally. In normal development, you do not inspect those files directly. You use consumers, Kafka UI, or admin tools.

To see the volume:

```bash
docker volume ls | grep kafka-event-lab
```

To reset everything:

```bash
docker compose down -v
```

That deletes Kafka topic data and starts fresh next time.

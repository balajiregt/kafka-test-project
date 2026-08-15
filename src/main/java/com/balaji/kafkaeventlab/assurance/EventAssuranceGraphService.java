package com.balaji.kafkaeventlab.assurance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventAssuranceGraphService {

    private final EventEvidenceSource eventEvidenceSource;
    private final ObjectMapper objectMapper;
    private final AssuranceProperties assuranceProperties;

    public EventAssuranceGraphService(
            EventEvidenceSource eventEvidenceSource,
            ObjectMapper objectMapper,
            AssuranceProperties assuranceProperties
    ) {
        this.eventEvidenceSource = eventEvidenceSource;
        this.objectMapper = objectMapper;
        this.assuranceProperties = assuranceProperties;
    }

    public AssuranceJourneyResponse journeyForSubject(UUID subjectId) {
        List<EventEvidence> trackedEvents = eventEvidenceSource.findBySubjectId(subjectId).stream()
                .sorted(eventOrdering())
                .toList();
        List<JourneyEvent> orderedEvents = trackedEvents.stream()
                .map(this::toJourneyEvent)
                .toList();

        Map<String, AssuranceNode> nodes = new LinkedHashMap<>();
        List<AssuranceEdge> edges = new ArrayList<>();
        String subjectNodeId = "subject:" + subjectId;
        nodes.put(subjectNodeId, new AssuranceNode(
                subjectNodeId,
                "SUBJECT",
                "Subject " + subjectId,
                Map.of("subjectId", subjectId.toString())
        ));

        JourneyEvent previous = null;
        for (JourneyEvent event : orderedEvents) {
            String eventNodeId = "event:" + event.eventId();
            nodes.put(eventNodeId, new AssuranceNode(
                    eventNodeId,
                    "EVENT",
                    valueOrUnknown(event.eventType()),
                    eventAttributes(event)
            ));

            String aggregateNodeId = aggregateNodeId(event);
            nodes.putIfAbsent(aggregateNodeId, new AssuranceNode(
                    aggregateNodeId,
                    valueOrUnknown(event.aggregateType()),
                    valueOrUnknown(event.aggregateType()) + " " + valueOrUnknown(event.aggregateId()),
                    Map.of("aggregateId", valueOrUnknown(event.aggregateId()))
            ));

            edges.add(new AssuranceEdge(aggregateNodeId, eventNodeId, "PRODUCED_EVENT", event.eventType()));
            edges.add(new AssuranceEdge(eventNodeId, subjectNodeId, "AFFECTS_SUBJECT", "affects subject"));
            addBusinessEdge(event, aggregateNodeId, subjectNodeId, edges);

            if (previous != null) {
                edges.add(new AssuranceEdge(
                        "event:" + previous.eventId(),
                        eventNodeId,
                        "FOLLOWS_EVENT",
                        previous.eventType() + " -> " + event.eventType()
                ));
            }
            previous = event;
        }

        return new AssuranceJourneyResponse(
                subjectId,
                List.copyOf(nodes.values()),
                List.copyOf(edges),
                orderedEvents,
                summary(orderedEvents, nodes, edges)
        );
    }

    private JourneyEvent toJourneyEvent(EventEvidence trackedEvent) {
        return new JourneyEvent(
                trackedEvent.getEventId(),
                trackedEvent.getEventType(),
                trackedEvent.getAggregateType(),
                trackedEvent.getAggregateId(),
                trackedEvent.getSubjectId(),
                trackedEvent.getCorrelationId(),
                trackedEvent.getTopicName(),
                trackedEvent.getPartitionNumber(),
                trackedEvent.getOffsetNumber(),
                trackedEvent.getOccurredAt(),
                trackedEvent.getConsumedAt(),
                trackedEvent.getSchemaVersion(),
                readData(trackedEvent.getEventJson())
        );
    }

    private Map<String, Object> readData(String eventJson) {
        if (eventJson == null || eventJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> eventPayload = objectMapper.readValue(eventJson, new TypeReference<>() {
            });
            Object data = eventPayload.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                return dataMap.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getKey().toString(),
                                Map.Entry::getValue,
                                (left, right) -> right,
                                LinkedHashMap::new
                        ));
            }
            return eventPayload;
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
    }

    private Comparator<EventEvidence> eventOrdering() {
        return Comparator
                .comparing(EventEvidence::getOccurredAt, Comparator.nullsLast(Instant::compareTo))
                .thenComparing(EventEvidence::getConsumedAt, Comparator.nullsLast(Instant::compareTo))
                .thenComparing(EventEvidence::getOffsetNumber, Comparator.nullsLast(Long::compareTo));
    }

    private Map<String, Object> eventAttributes(JourneyEvent event) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("eventId", valueOrUnknown(event.eventId()));
        attributes.put("eventType", event.eventType());
        attributes.put("aggregateType", event.aggregateType());
        attributes.put("aggregateId", valueOrUnknown(event.aggregateId()));
        attributes.put("subjectId", valueOrUnknown(event.subjectId()));
        attributes.put("correlationId", event.correlationId());
        attributes.put("schemaVersion", event.schemaVersion());
        attributes.put("topicName", event.topicName());
        attributes.put("partitionNumber", event.partitionNumber());
        attributes.put("offsetNumber", event.offsetNumber());
        attributes.put("occurredAt", event.occurredAt());
        attributes.put("consumedAt", event.consumedAt());
        attributes.put("dataFieldCount", event.data() == null ? 0 : event.data().size());
        return attributes;
    }

    private String aggregateNodeId(JourneyEvent event) {
        return valueOrUnknown(event.aggregateType()).toLowerCase() + ":" + valueOrUnknown(event.aggregateId());
    }

    private void addBusinessEdge(
            JourneyEvent event,
            String aggregateNodeId,
            String userNodeId,
            List<AssuranceEdge> edges
    ) {
        assuranceProperties.getGraphEdgeRules().stream()
                .filter(rule -> rule.getWhenAggregateType() != null)
                .filter(rule -> rule.getWhenAggregateType().equals(event.aggregateType()))
                .filter(rule -> rule.getEdgeType() != null && !rule.getEdgeType().isBlank())
                .map(rule -> new AssuranceEdge(
                        aggregateNodeId,
                        userNodeId,
                        rule.getEdgeType(),
                        render(rule.getLabel(), event)
                ))
                .forEach(edges::add);
    }

    private String render(String template, JourneyEvent event) {
        return (template == null ? "" : template)
                .replace("{eventType}", valueOrUnknown(event.eventType()))
                .replace("{aggregateType}", valueOrUnknown(event.aggregateType()))
                .replace("{aggregateId}", valueOrUnknown(event.aggregateId()))
                .replace("{subjectId}", valueOrUnknown(event.subjectId()));
    }

    private String valueOrUnknown(Object value) {
        return value == null ? "UNKNOWN" : value.toString();
    }

    private JourneySummary summary(
            List<JourneyEvent> orderedEvents,
            Map<String, AssuranceNode> nodes,
            List<AssuranceEdge> edges
    ) {
        Map<String, Long> eventCountByType = orderedEvents.stream()
                .collect(Collectors.groupingBy(
                        event -> valueOrUnknown(event.eventType()),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        List<String> eventTypes = orderedEvents.stream()
                .map(event -> valueOrUnknown(event.eventType()))
                .distinct()
                .toList();
        Instant firstEventAt = orderedEvents.isEmpty() ? null : orderedEvents.get(0).occurredAt();
        Instant lastEventAt = orderedEvents.isEmpty() ? null : orderedEvents.get(orderedEvents.size() - 1).occurredAt();
        return new JourneySummary(
                orderedEvents.size(),
                nodes.size(),
                edges.size(),
                firstEventAt,
                lastEventAt,
                eventTypes,
                eventCountByType,
                graphMetrics(orderedEvents, nodes, edges, firstEventAt, lastEventAt)
        );
    }

    private AssuranceGraphMetrics graphMetrics(
            List<JourneyEvent> orderedEvents,
            Map<String, AssuranceNode> nodes,
            List<AssuranceEdge> edges,
            Instant firstEventAt,
            Instant lastEventAt
    ) {
        long producedEventEdges = edges.stream()
                .filter(edge -> "PRODUCED_EVENT".equals(edge.edgeType()))
                .count();
        long sequenceEdges = edges.stream()
                .filter(edge -> "FOLLOWS_EVENT".equals(edge.edgeType()))
                .count();
        int metadataCompleteEvents = (int) orderedEvents.stream()
                .filter(this::hasRequiredMetadata)
                .count();
        Long averageConsumeLagMillis = averageConsumeLagMillis(orderedEvents);
        String mostConnectedNodeId = mostConnectedNodeId(edges);

        return new AssuranceGraphMetrics(
                (int) nodes.values().stream().filter(node -> "EVENT".equals(node.nodeType())).count(),
                (int) nodes.values().stream().filter(node -> !"EVENT".equals(node.nodeType())).count(),
                Math.toIntExact(producedEventEdges),
                Math.toIntExact(sequenceEdges),
                metadataCompleteEvents,
                orderedEvents.size() - metadataCompleteEvents,
                offsetSpan(orderedEvents),
                averageConsumeLagMillis,
                journeyDurationMillis(firstEventAt, lastEventAt),
                mostConnectedNodeId,
                mostConnectedNodeId == null ? 0 : nodeDegree(mostConnectedNodeId, edges)
        );
    }

    private boolean hasRequiredMetadata(JourneyEvent event) {
        Map<String, Object> eventValues = objectMapper.convertValue(event, new TypeReference<>() {
        });
        return assuranceProperties.getMetadataRule().getRequiredFields().stream()
                .allMatch(field -> hasValue(eventValues.get(field)));
    }

    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.isBlank();
        }
        return true;
    }

    private Long averageConsumeLagMillis(List<JourneyEvent> orderedEvents) {
        List<Long> lags = orderedEvents.stream()
                .filter(event -> event.occurredAt() != null && event.consumedAt() != null)
                .map(event -> event.consumedAt().toEpochMilli() - event.occurredAt().toEpochMilli())
                .toList();
        if (lags.isEmpty()) {
            return null;
        }
        return Math.round(lags.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private long offsetSpan(List<JourneyEvent> orderedEvents) {
        List<Long> offsets = orderedEvents.stream()
                .map(JourneyEvent::offsetNumber)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();
        if (offsets.isEmpty()) {
            return 0;
        }
        return offsets.get(offsets.size() - 1) - offsets.get(0);
    }

    private Long journeyDurationMillis(Instant firstEventAt, Instant lastEventAt) {
        if (firstEventAt == null || lastEventAt == null) {
            return null;
        }
        return lastEventAt.toEpochMilli() - firstEventAt.toEpochMilli();
    }

    private String mostConnectedNodeId(List<AssuranceEdge> edges) {
        return edges.stream()
                .flatMap(edge -> List.of(edge.fromNodeId(), edge.toNodeId()).stream())
                .collect(Collectors.groupingBy(nodeId -> nodeId, LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private int nodeDegree(String nodeId, List<AssuranceEdge> edges) {
        return (int) edges.stream()
                .filter(edge -> nodeId.equals(edge.fromNodeId()) || nodeId.equals(edge.toNodeId()))
                .count();
    }
}

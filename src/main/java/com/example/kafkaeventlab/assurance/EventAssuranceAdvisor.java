package com.example.kafkaeventlab.assurance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class EventAssuranceAdvisor {

    private final AssuranceProperties assuranceProperties;
    private final ObjectMapper objectMapper;

    public EventAssuranceAdvisor(AssuranceProperties assuranceProperties, ObjectMapper objectMapper) {
        this.assuranceProperties = assuranceProperties;
        this.objectMapper = objectMapper;
    }

    public List<AssuranceFinding> findings(AssuranceJourneyResponse journey) {
        List<AssuranceFinding> findings = new ArrayList<>();
        List<JourneyEvent> events = journey.orderedEvents();

        if (assuranceProperties.getEventPresenceRule().isEnabled()) {
            findings.add(eventsPresent(events, assuranceProperties.getEventPresenceRule()));
        }
        if (assuranceProperties.getMetadataRule().isEnabled()) {
            findings.add(metadataComplete(events, assuranceProperties.getMetadataRule()));
        }
        assuranceProperties.getOrderingRules().stream()
                .filter(this::isUsableOrderingRule)
                .map(rule -> orderingRule(events, rule))
                .forEach(findings::add);
        assuranceProperties.getEventFlagRules().stream()
                .filter(this::isUsableEventFlagRule)
                .map(rule -> eventFlagRule(events, rule))
                .forEach(findings::add);

        return findings;
    }

    public List<TestingRecommendation> testingRecommendations() {
        return testingRecommendations(null);
    }

    public List<TestingRecommendation> testingRecommendations(AssuranceJourneyResponse journey) {
        List<TestingRecommendation> recommendations = new ArrayList<>();
        List<AssuranceFinding> journeyFindings = journey == null ? List.of() : findings(journey);

        if (assuranceProperties.getEventPresenceRule().isEnabled()) {
            recommendations.add(eventEvidenceRecommendation(journey));
        }
        if (assuranceProperties.getMetadataRule().isEnabled()) {
            recommendations.add(metadataRecommendation(journey, journeyFindings));
        }
        assuranceProperties.getOrderingRules().stream()
                .filter(this::isUsableOrderingRule)
                .map(rule -> orderingRecommendation(rule, journeyFindings))
                .forEach(recommendations::add);
        assuranceProperties.getEventFlagRules().stream()
                .filter(this::isUsableEventFlagRule)
                .map(rule -> eventFlagRecommendation(rule, journeyFindings))
                .forEach(recommendations::add);
        assuranceProperties.getGraphEdgeRules().stream()
                .filter(rule -> hasText(rule.getWhenAggregateType()) && hasText(rule.getEdgeType()))
                .map(this::graphRecommendation)
                .forEach(recommendations::add);
        assuranceProperties.getTestingRecommendations().stream()
                .map(recommendation -> new TestingRecommendation(
                        recommendation.getCapability(),
                        recommendation.getTestLayer(),
                        recommendation.getWhyHere(),
                        recommendation.getExampleAssertions()
                ))
                .forEach(recommendations::add);

        return uniqueRecommendations(recommendations)
                .values()
                .stream()
                .toList();
    }

    private LinkedHashMap<String, TestingRecommendation> uniqueRecommendations(
            List<TestingRecommendation> recommendations
    ) {
        LinkedHashMap<String, TestingRecommendation> unique = new LinkedHashMap<>();
        recommendations.forEach(recommendation -> unique.putIfAbsent(recommendationKey(recommendation), recommendation));
        return unique;
    }

    private String recommendationKey(TestingRecommendation recommendation) {
        return recommendation.testLayer() + "|" + recommendation.capability();
    }

    private TestingRecommendation eventEvidenceRecommendation(AssuranceJourneyResponse journey) {
        AssuranceProperties.EventPresenceRule rule = assuranceProperties.getEventPresenceRule();
        int eventCount = journey == null ? 0 : journey.orderedEvents().size();
        String whyHere = journey == null
                ? "Generated from the event presence rule; prove the configured evidence collector can observe consumed events."
                : "Generated from the event presence rule; this journey currently has " + eventCount + " consumed event(s).";
        return new TestingRecommendation(
                "Consumed event evidence capture",
                rule.getRecommendedTestLayer(),
                whyHere,
                List.of(
                        "After a business operation, the evidence source records at least one consumed event for the subject.",
                        "Recorded evidence includes topic, partition, offset, timestamps, and payload JSON.",
                        "The report can rebuild an ordered journey from the collected evidence."
                )
        );
    }

    private TestingRecommendation metadataRecommendation(
            AssuranceJourneyResponse journey,
            List<AssuranceFinding> journeyFindings
    ) {
        AssuranceProperties.MetadataRule rule = assuranceProperties.getMetadataRule();
        String requiredFields = String.join(", ", rule.getRequiredFields());
        String status = statusFor(rule.getRuleId(), journeyFindings);
        String whyHere = journey == null
                ? "Generated from metadata rule " + rule.getRuleId() + "; required fields are " + requiredFields + "."
                : "Generated from metadata rule " + rule.getRuleId()
                + "; current journey status is " + status
                + " with " + missingMetadataGaps(journey) + " metadata gap(s).";
        return new TestingRecommendation(
                "Event metadata contract: " + requiredFields,
                rule.getRecommendedTestLayer(),
                whyHere,
                List.of(
                        "Validate required metadata fields on every produced event: " + requiredFields + ".",
                        "Reject or flag events with blank correlation id, missing schema version, or missing consumed offset.",
                        "Run schema/contract checks before UI/e2e flows depend on these events."
                )
        );
    }

    private TestingRecommendation orderingRecommendation(
            AssuranceProperties.OrderingRule rule,
            List<AssuranceFinding> journeyFindings
    ) {
        String status = statusFor(rule.getRuleId(), journeyFindings);
        return new TestingRecommendation(
                "Lifecycle order: " + rule.getRequiredPriorEventType() + " before " + rule.getTriggerEventType(),
                rule.getRecommendedTestLayer(),
                "Generated from ordering rule " + rule.getRuleId() + currentStatus(status) + ".",
                List.of(
                        "Positive path: " + rule.getRequiredPriorEventType() + " appears before " + rule.getTriggerEventType() + " for the same subject.",
                        "Negative path: " + rule.getTriggerEventType() + " without prior " + rule.getRequiredPriorEventType() + " is flagged.",
                        "Keep this below UI/e2e unless visible browser behavior is the thing under test."
                )
        );
    }

    private TestingRecommendation eventFlagRecommendation(
            AssuranceProperties.EventFlagRule rule,
            List<AssuranceFinding> journeyFindings
    ) {
        String status = statusFor(rule.getRuleId(), journeyFindings);
        return new TestingRecommendation(
                "Flagged event behavior: " + rule.getEventType(),
                rule.getRecommendedTestLayer(),
                "Generated from event flag rule " + rule.getRuleId() + currentStatus(status) + ".",
                List.of(
                        "When " + rule.getEventType() + " appears, assert the domain reason and stored state.",
                        "Classify expected flagged events as warnings when they are valid business outcomes.",
                        "Use UI/e2e only for the user-visible message or recovery journey."
                )
        );
    }

    private TestingRecommendation graphRecommendation(AssuranceProperties.GraphEdgeRule rule) {
        return new TestingRecommendation(
                "Graph relationship: " + rule.getWhenAggregateType() + " creates " + rule.getEdgeType(),
                "Kafka integration",
                "Generated from graph edge mapping " + rule.getWhenAggregateType() + " -> " + rule.getEdgeType() + ".",
                List.of(
                        "Consumed " + rule.getWhenAggregateType() + " evidence creates a " + rule.getEdgeType() + " edge in the journey graph.",
                        "The graph highlights connected subject, aggregate, and event nodes for this relationship.",
                        "Validate relationship mapping with focused graph/advisor tests, not browser-only checks."
                )
        );
    }

    private String statusFor(String ruleId, List<AssuranceFinding> findings) {
        return findings.stream()
                .filter(finding -> ruleId.equals(finding.ruleId()))
                .map(AssuranceFinding::status)
                .findFirst()
                .orElse("not evaluated");
    }

    private String currentStatus(String status) {
        return "not evaluated".equals(status) ? "" : "; current journey status is " + status;
    }

    private int missingMetadataGaps(AssuranceJourneyResponse journey) {
        if (journey.summary() == null || journey.summary().graphMetrics() == null) {
            return 0;
        }
        return journey.summary().graphMetrics().missingMetadataEvents();
    }

    private AssuranceFinding eventsPresent(
            List<JourneyEvent> events,
            AssuranceProperties.EventPresenceRule rule
    ) {
        if (events.isEmpty()) {
            return finding(
                    rule.getRuleId(),
                    rule.getEmptySeverity(),
                    rule.getEmptyStatus(),
                    rule.getEmptyMessage(),
                    List.of(),
                    rule.getRecommendedTestLayer()
            );
        }
        return finding(
                rule.getRuleId(),
                rule.getPresentSeverity(),
                rule.getPresentStatus(),
                rule.getPresentMessage(),
                eventIds(events),
                rule.getRecommendedTestLayer()
        );
    }

    private AssuranceFinding metadataComplete(
            List<JourneyEvent> events,
            AssuranceProperties.MetadataRule rule
    ) {
        List<UUID> incomplete = events.stream()
                .filter(Predicate.not(event -> hasRequiredMetadata(event, rule.getRequiredFields())))
                .map(JourneyEvent::eventId)
                .toList();
        Map<String, String> tokens = Map.of("requiredFields", String.join(", ", rule.getRequiredFields()));
        if (!incomplete.isEmpty()) {
            return finding(
                    rule.getRuleId(),
                    rule.getSeverityOnMissing(),
                    rule.getStatusOnMissing(),
                    render(rule.getMissingMessage(), tokens),
                    incomplete,
                    rule.getRecommendedTestLayer()
            );
        }
        return finding(
                rule.getRuleId(),
                rule.getSeverityOnPass(),
                rule.getStatusOnPass(),
                render(rule.getPassMessage(), tokens),
                eventIds(events),
                rule.getRecommendedTestLayer()
        );
    }

    private AssuranceFinding orderingRule(
            List<JourneyEvent> events,
            AssuranceProperties.OrderingRule rule
    ) {
        Map<String, String> tokens = Map.of(
                "triggerEventType", rule.getTriggerEventType(),
                "requiredPriorEventType", rule.getRequiredPriorEventType()
        );
        int trigger = indexOf(events, event -> rule.getTriggerEventType().equals(event.eventType()));
        if (trigger < 0) {
            return finding(
                    rule.getRuleId(),
                    rule.getSeverityOnPass(),
                    rule.getStatusWhenNotApplicable(),
                    render(rule.getNotApplicableMessage(), tokens),
                    List.of(),
                    rule.getRecommendedTestLayer()
            );
        }
        int requiredPrior = indexOf(events, event -> rule.getRequiredPriorEventType().equals(event.eventType()));
        if (requiredPrior >= 0 && requiredPrior < trigger) {
            return finding(
                    rule.getRuleId(),
                    rule.getSeverityOnPass(),
                    rule.getStatusOnPass(),
                    render(rule.getPassMessage(), tokens),
                    List.of(events.get(requiredPrior).eventId(), events.get(trigger).eventId()),
                    rule.getRecommendedTestLayer()
            );
        }
        return finding(
                rule.getRuleId(),
                rule.getSeverityOnFailure(),
                rule.getStatusOnFailure(),
                render(rule.getFailureMessage(), tokens),
                List.of(events.get(trigger).eventId()),
                rule.getRecommendedTestLayer()
        );
    }

    private AssuranceFinding eventFlagRule(
            List<JourneyEvent> events,
            AssuranceProperties.EventFlagRule rule
    ) {
        List<JourneyEvent> matchingEvents = events.stream()
                .filter(event -> rule.getEventType().equals(event.eventType()))
                .toList();
        Map<String, String> tokens = Map.of("eventType", rule.getEventType());
        if (matchingEvents.isEmpty()) {
            return finding(
                    rule.getRuleId(),
                    rule.getSeverityWhenAbsent(),
                    rule.getStatusWhenAbsent(),
                    render(rule.getAbsentMessage(), tokens),
                    List.of(),
                    rule.getRecommendedTestLayer()
            );
        }
        return finding(
                rule.getRuleId(),
                rule.getSeverityWhenPresent(),
                rule.getStatusWhenPresent(),
                render(rule.getPresentMessage(), tokens),
                eventIds(matchingEvents),
                rule.getRecommendedTestLayer()
        );
    }

    private boolean hasRequiredMetadata(JourneyEvent event, List<String> requiredFields) {
        Map<String, Object> eventValues = objectMapper.convertValue(event, new TypeReference<>() {
        });
        return requiredFields.stream()
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

    private boolean isUsableOrderingRule(AssuranceProperties.OrderingRule rule) {
        return hasText(rule.getRuleId())
                && hasText(rule.getTriggerEventType())
                && hasText(rule.getRequiredPriorEventType());
    }

    private boolean isUsableEventFlagRule(AssuranceProperties.EventFlagRule rule) {
        return hasText(rule.getRuleId()) && hasText(rule.getEventType());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int indexOf(List<JourneyEvent> events, Predicate<JourneyEvent> predicate) {
        for (int index = 0; index < events.size(); index++) {
            if (predicate.test(events.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private List<UUID> eventIds(List<JourneyEvent> events) {
        return events.stream()
                .map(JourneyEvent::eventId)
                .filter(Objects::nonNull)
                .toList();
    }

    private String render(String template, Map<String, String> values) {
        String rendered = template == null ? "" : template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return rendered;
    }

    private AssuranceFinding finding(
            String ruleId,
            String severity,
            String status,
            String message,
            List<UUID> evidenceEventIds,
            String recommendedTestLayer
    ) {
        return new AssuranceFinding(ruleId, severity, status, message, evidenceEventIds, recommendedTestLayer);
    }
}

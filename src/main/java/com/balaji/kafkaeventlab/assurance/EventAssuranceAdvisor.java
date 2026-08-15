package com.balaji.kafkaeventlab.assurance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        return assuranceProperties.getTestingRecommendations().stream()
                .map(recommendation -> new TestingRecommendation(
                        recommendation.getCapability(),
                        recommendation.getTestLayer(),
                        recommendation.getWhyHere(),
                        recommendation.getExampleAssertions()
                ))
                .toList();
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

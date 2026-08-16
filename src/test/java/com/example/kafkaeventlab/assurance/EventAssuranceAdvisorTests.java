package com.example.kafkaeventlab.assurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventAssuranceAdvisorTests {

    private final AssuranceProperties assuranceProperties = configuredRules();
    private final EventAssuranceAdvisor advisor = new EventAssuranceAdvisor(
            assuranceProperties,
            new ObjectMapper().registerModule(new JavaTimeModule())
    );
    private final UUID subjectId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-08-14T10:00:00Z");

    @Test
    void configuredOrderingRulesReturnPassingFindings() {
        List<AssuranceFinding> findings = advisor.findings(journey(
                event("ENTITY_CREATED", "ANY_ENTITY", subjectId, 0),
                event("ENTITY_APPROVED", "ANY_ENTITY", subjectId, 1)
        ));

        assertThat(findings)
                .filteredOn(finding -> finding.ruleId().equals("generic.created-before-approved"))
                .singleElement()
                .satisfies(finding -> assertThat(finding.status()).isEqualTo("PASS"));
    }

    @Test
    void configuredOrderingRulesFlagMissingPriorEvents() {
        List<AssuranceFinding> findings = advisor.findings(journey(
                event("ENTITY_APPROVED", "ANY_ENTITY", subjectId, 0)
        ));

        assertThat(findings)
                .filteredOn(finding -> finding.ruleId().equals("generic.created-before-approved"))
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.status()).isEqualTo("FAIL");
                    assertThat(finding.recommendedTestLayer()).isEqualTo("Kafka integration");
                });
    }

    @Test
    void configuredEventFlagRulesReturnWarningsWhenEventIsPresent() {
        List<AssuranceFinding> findings = advisor.findings(journey(
                event("ENTITY_REJECTED", "ANY_ENTITY", subjectId, 0)
        ));

        assertThat(findings)
                .filteredOn(finding -> finding.ruleId().equals("generic.rejections-reviewed"))
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.status()).isEqualTo("WARN");
                    assertThat(finding.evidenceEventIds()).hasSize(1);
                });
    }

    @Test
    void configuredMetadataFieldsAreEvaluatedDynamically() {
        JourneyEvent incompleteEvent = new JourneyEvent(
                UUID.randomUUID(),
                "ENTITY_CREATED",
                "ANY_ENTITY",
                subjectId,
                subjectId,
                null,
                "events.v1",
                0,
                null,
                start,
                start,
                null,
                Map.of()
        );

        List<AssuranceFinding> findings = advisor.findings(journey(incompleteEvent));

        assertThat(findings)
                .filteredOn(finding -> finding.ruleId().equals("generic.required-metadata"))
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.status()).isEqualTo("FAIL");
                    assertThat(finding.message()).contains("eventType", "correlationId", "offsetNumber");
                });
    }

    private AssuranceProperties configuredRules() {
        AssuranceProperties properties = new AssuranceProperties();

        properties.getMetadataRule().setRuleId("generic.required-metadata");
        properties.getMetadataRule().setRequiredFields(List.of(
                "eventType",
                "aggregateId",
                "subjectId",
                "correlationId",
                "schemaVersion",
                "offsetNumber"
        ));

        AssuranceProperties.OrderingRule orderingRule = new AssuranceProperties.OrderingRule();
        orderingRule.setRuleId("generic.created-before-approved");
        orderingRule.setTriggerEventType("ENTITY_APPROVED");
        orderingRule.setRequiredPriorEventType("ENTITY_CREATED");
        orderingRule.setRecommendedTestLayer("Kafka integration");
        properties.setOrderingRules(List.of(orderingRule));

        AssuranceProperties.EventFlagRule eventFlagRule = new AssuranceProperties.EventFlagRule();
        eventFlagRule.setRuleId("generic.rejections-reviewed");
        eventFlagRule.setEventType("ENTITY_REJECTED");
        eventFlagRule.setStatusWhenPresent("WARN");
        eventFlagRule.setRecommendedTestLayer("API integration");
        properties.setEventFlagRules(List.of(eventFlagRule));

        return properties;
    }

    private AssuranceJourneyResponse journey(JourneyEvent... events) {
        return new AssuranceJourneyResponse(
                subjectId,
                List.of(),
                List.of(),
                List.of(events),
                new JourneySummary(
                        events.length,
                        0,
                        0,
                        start,
                        start.plusSeconds(events.length),
                        List.of(),
                        Map.of(),
                        new AssuranceGraphMetrics(0, 0, 0, 0, events.length, 0, 0, 10L, events.length * 1000L, null, 0)
                )
        );
    }

    private JourneyEvent event(String eventType, String aggregateType, UUID aggregateId, long offset) {
        Instant eventTime = start.plusSeconds(offset);
        return new JourneyEvent(
                UUID.randomUUID(),
                eventType,
                aggregateType,
                aggregateId,
                subjectId,
                UUID.randomUUID().toString(),
                "events.v1",
                0,
                offset,
                eventTime,
                eventTime.plusMillis(10),
                1,
                Map.of()
        );
    }
}

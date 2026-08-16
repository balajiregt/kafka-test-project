package com.balaji.kafkaeventlab.assurance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssuranceDemoScenarioService {

    private static final String TOPIC = "demo.assurance.events.v1";

    private final EventEvidenceRepository eventEvidenceRepository;
    private final EventAssuranceGraphService graphService;
    private final EventAssuranceAdvisor advisor;
    private final AssuranceProperties assuranceProperties;
    private final ObjectMapper objectMapper;

    public AssuranceDemoScenarioService(
            EventEvidenceRepository eventEvidenceRepository,
            EventAssuranceGraphService graphService,
            EventAssuranceAdvisor advisor,
            AssuranceProperties assuranceProperties,
            ObjectMapper objectMapper
    ) {
        this.eventEvidenceRepository = eventEvidenceRepository;
        this.graphService = graphService;
        this.advisor = advisor;
        this.assuranceProperties = assuranceProperties;
        this.objectMapper = objectMapper;
    }

    public List<String> scenarios() {
        return List.of(
                "incorrect-sign-in-sequence",
                "missing-metadata",
                "reset-completed-without-request",
                "failed-sign-in-warning"
        );
    }

    public AssuranceDemoScenarioResponse create(String scenario) {
        if (!assuranceProperties.getDemoScenarios().isEnabled()) {
            throw new IllegalStateException("Assurance demo scenarios are disabled.");
        }
        UUID subjectId = UUID.randomUUID();
        Instant start = Instant.now();
        List<EventEvidence> evidence = switch (scenario) {
            case "incorrect-sign-in-sequence" -> incorrectSignInSequence(subjectId, start);
            case "missing-metadata" -> missingMetadata(subjectId, start);
            case "reset-completed-without-request" -> resetCompletedWithoutRequest(subjectId, start);
            case "failed-sign-in-warning" -> failedSignInWarning(subjectId, start);
            default -> throw new IllegalArgumentException("Unknown assurance demo scenario: " + scenario);
        };
        eventEvidenceRepository.saveAll(evidence);
        AssuranceJourneyResponse journey = graphService.journeyForSubject(subjectId);
        return new AssuranceDemoScenarioResponse(
                scenario,
                subjectId,
                "/assurance-report.html?subjectId=" + subjectId,
                journey.summary(),
                advisor.findings(journey)
        );
    }

    private List<EventEvidence> incorrectSignInSequence(UUID subjectId, Instant start) {
        return List.of(
                event("USER_REGISTERED", "USER", subjectId, subjectId, start, 0, Map.of("status", "ACTIVE")),
                event("SIGN_IN_SUCCEEDED", "SIGN_IN_ATTEMPT", UUID.randomUUID(), subjectId, start, 1, Map.of("result", "SUCCESS"))
        );
    }

    private List<EventEvidence> missingMetadata(UUID subjectId, Instant start) {
        return List.of(new EventEvidence(
                UUID.randomUUID(),
                "USER_REGISTERED",
                "USER",
                subjectId,
                subjectId,
                null,
                TOPIC,
                0,
                null,
                start,
                start.plusMillis(25),
                null,
                json("USER_REGISTERED", "USER", subjectId, subjectId, Map.of("status", "PENDING_ACTIVATION"))
        ));
    }

    private List<EventEvidence> resetCompletedWithoutRequest(UUID subjectId, Instant start) {
        UUID resetId = UUID.randomUUID();
        return List.of(
                event("USER_REGISTERED", "USER", subjectId, subjectId, start, 0, Map.of("status", "ACTIVE")),
                event("PASSWORD_RESET_COMPLETED", "PASSWORD_RESET", resetId, subjectId, start, 1, Map.of("status", "COMPLETED"))
        );
    }

    private List<EventEvidence> failedSignInWarning(UUID subjectId, Instant start) {
        return List.of(
                event("USER_REGISTERED", "USER", subjectId, subjectId, start, 0, Map.of("status", "ACTIVE")),
                event("SIGN_IN_FAILED", "SIGN_IN_ATTEMPT", UUID.randomUUID(), subjectId, start, 1, Map.of("result", "FAILURE", "reason", "Credential was not accepted"))
        );
    }

    private EventEvidence event(
            String eventType,
            String aggregateType,
            UUID aggregateId,
            UUID subjectId,
            Instant start,
            long offset,
            Map<String, Object> data
    ) {
        return new EventEvidence(
                UUID.randomUUID(),
                eventType,
                aggregateType,
                aggregateId,
                subjectId,
                UUID.randomUUID().toString(),
                TOPIC,
                0,
                offset,
                start.plusSeconds(offset),
                start.plusSeconds(offset).plusMillis(25),
                1,
                json(eventType, aggregateType, aggregateId, subjectId, data)
        );
    }

    private String json(
            String eventType,
            String aggregateType,
            UUID aggregateId,
            UUID subjectId,
            Map<String, Object> data
    ) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "eventType", eventType,
                    "aggregateType", aggregateType,
                    "aggregateId", aggregateId,
                    "subjectId", subjectId,
                    "data", data
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to create demo event JSON", exception);
        }
    }
}

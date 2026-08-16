package com.balaji.kafkaeventlab.assurance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class AssurancePlaygroundService {

    private final EventEvidenceRepository eventEvidenceRepository;
    private final EventAssuranceGraphService graphService;
    private final EventAssuranceAdvisor advisor;
    private final AssuranceProperties assuranceProperties;
    private final ObjectMapper objectMapper;

    public AssurancePlaygroundService(
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

    public AssurancePlaygroundResponse submit(AssurancePlaygroundRequest request) {
        if (!assuranceProperties.getPlayground().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assurance evidence playground is disabled.");
        }
        if (request == null || request.events() == null || request.events().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one event evidence item is required.");
        }

        UUID subjectId = subjectId(request);
        eventEvidenceRepository.deleteAll(eventEvidenceRepository.findBySubjectId(subjectId));
        eventEvidenceRepository.saveAll(toEvidence(request.events(), subjectId));

        AssuranceJourneyResponse journey = graphService.journeyForSubject(subjectId);
        return new AssurancePlaygroundResponse(
                scenarioName(request.scenarioName()),
                subjectId,
                "/assurance-report.html?subjectId=" + subjectId,
                journey.summary(),
                advisor.findings(journey)
        );
    }

    private UUID subjectId(AssurancePlaygroundRequest request) {
        if (request.subjectId() != null) {
            return request.subjectId();
        }
        return request.events().stream()
                .map(AssurancePlaygroundEventRequest::subjectId)
                .filter(value -> value != null)
                .findFirst()
                .orElseGet(UUID::randomUUID);
    }

    private List<EventEvidence> toEvidence(List<AssurancePlaygroundEventRequest> requests, UUID subjectId) {
        Instant start = Instant.now();
        return IntStream.range(0, requests.size())
                .mapToObj(index -> toEvidence(requests.get(index), subjectId, start, index))
                .toList();
    }

    private EventEvidence toEvidence(
            AssurancePlaygroundEventRequest request,
            UUID subjectId,
            Instant start,
            int index
    ) {
        Instant occurredAt = request.occurredAt() == null ? start.plusSeconds(index) : request.occurredAt();
        Instant consumedAt = request.consumedAt() == null ? null : request.consumedAt();
        return new EventEvidence(
                request.eventId() == null ? UUID.randomUUID() : request.eventId(),
                request.eventType(),
                request.aggregateType(),
                request.aggregateId(),
                subjectId,
                request.correlationId(),
                hasText(request.topicName()) ? request.topicName() : assuranceProperties.getPlayground().getDefaultTopic(),
                request.partitionNumber(),
                request.offsetNumber(),
                occurredAt,
                consumedAt,
                request.schemaVersion(),
                eventJson(request, subjectId)
        );
    }

    private String eventJson(AssurancePlaygroundEventRequest request, UUID subjectId) {
        if (hasText(request.eventJson())) {
            return request.eventJson();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", request.eventId());
        payload.put("eventType", request.eventType());
        payload.put("aggregateType", request.aggregateType());
        payload.put("aggregateId", request.aggregateId());
        payload.put("subjectId", subjectId);
        payload.put("correlationId", request.correlationId());
        payload.put("schemaVersion", request.schemaVersion());
        payload.put("data", request.data() == null ? Map.of() : request.data());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to serialize event evidence.", exception);
        }
    }

    private String scenarioName(String scenarioName) {
        return hasText(scenarioName) ? scenarioName : "custom-evidence";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

package com.example.kafkaeventlab.assurance;

import java.util.List;
import java.util.UUID;

public record AssuranceJourneyResponse(
        UUID subjectId,
        List<AssuranceNode> nodes,
        List<AssuranceEdge> edges,
        List<JourneyEvent> orderedEvents,
        JourneySummary summary
) {
}

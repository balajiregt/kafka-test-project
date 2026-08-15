package com.balaji.kafkaeventlab.assurance;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record JourneySummary(
        int totalEvents,
        int totalNodes,
        int totalEdges,
        Instant firstEventAt,
        Instant lastEventAt,
        List<String> eventTypes,
        Map<String, Long> eventCountByType,
        AssuranceGraphMetrics graphMetrics
) {
}

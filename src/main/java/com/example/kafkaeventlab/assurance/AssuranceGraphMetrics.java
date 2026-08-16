package com.example.kafkaeventlab.assurance;

public record AssuranceGraphMetrics(
        int eventNodeCount,
        int domainNodeCount,
        int producedEventEdges,
        int sequenceEdges,
        int metadataCompleteEvents,
        int missingMetadataEvents,
        long offsetSpan,
        Long averageConsumeLagMillis,
        Long journeyDurationMillis,
        String mostConnectedNodeId,
        int mostConnectedNodeDegree
) {
}

package com.example.kafkaeventlab.assurance;

public record AssuranceEdge(
        String fromNodeId,
        String toNodeId,
        String edgeType,
        String label
) {
}

package com.balaji.kafkaeventlab.assurance;

import java.util.Map;

public record AssuranceNode(
        String nodeId,
        String nodeType,
        String label,
        Map<String, Object> attributes
) {
}

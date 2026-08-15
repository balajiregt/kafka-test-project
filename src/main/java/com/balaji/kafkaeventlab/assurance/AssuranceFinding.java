package com.balaji.kafkaeventlab.assurance;

import java.util.List;
import java.util.UUID;

public record AssuranceFinding(
        String ruleId,
        String severity,
        String status,
        String message,
        List<UUID> evidenceEventIds,
        String recommendedTestLayer
) {
}

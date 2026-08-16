package com.balaji.kafkaeventlab.assurance;

import java.util.List;
import java.util.UUID;

public record AssurancePlaygroundRequest(
        String scenarioName,
        UUID subjectId,
        List<AssurancePlaygroundEventRequest> events
) {
}

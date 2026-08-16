package com.example.kafkaeventlab.assurance;

import java.util.List;
import java.util.UUID;

public record AssurancePlaygroundResponse(
        String scenarioName,
        UUID subjectId,
        String reportPath,
        JourneySummary summary,
        List<AssuranceFinding> findings
) {
}

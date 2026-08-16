package com.example.kafkaeventlab.assurance;

import java.util.List;

public record AssuranceAssistantRequest(
        String message,
        List<AssuranceAssistantContextSource> contextSources,
        Boolean includeProjectContext,
        String apiKey,
        String model,
        String baseUrl
) {
}

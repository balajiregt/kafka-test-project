package com.balaji.kafkaeventlab.assurance;

import java.util.List;

public record AssuranceAssistantRequest(
        String message,
        List<AssuranceAssistantContextSource> contextSources,
        String apiKey,
        String model,
        String baseUrl
) {
}

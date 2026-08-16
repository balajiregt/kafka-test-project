package com.balaji.kafkaeventlab.assurance;

import java.util.List;

public record AssuranceAssistantResponse(
        String status,
        String mode,
        String reply,
        String suggestedYaml,
        List<String> warnings,
        List<String> acceptedContextSources
) {
}

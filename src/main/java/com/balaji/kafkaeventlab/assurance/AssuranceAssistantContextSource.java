package com.balaji.kafkaeventlab.assurance;

public record AssuranceAssistantContextSource(
        String sourceType,
        String name,
        String content
) {
}

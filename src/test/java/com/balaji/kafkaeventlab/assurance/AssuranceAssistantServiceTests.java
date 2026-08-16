package com.balaji.kafkaeventlab.assurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssuranceAssistantServiceTests {

    private final AssuranceAssistantService assistantService = new AssuranceAssistantService(
            new AssuranceProperties(),
            new ObjectMapper()
    );

    @Test
    void refusesUnrelatedRequests() {
        AssuranceAssistantResponse response = assistantService.advise(new AssuranceAssistantRequest(
                "Plan a weekend trip",
                List.of(),
                null,
                null,
                null
        ));

        assertThat(response.status()).isEqualTo("REFUSED");
        assertThat(response.mode()).isEqualTo("guardrail");
        assertThat(response.suggestedYaml()).isBlank();
    }

    @Test
    void returnsDeterministicYamlWhenNoApiKeyIsProvided() {
        AssuranceAssistantResponse response = assistantService.advise(new AssuranceAssistantRequest(
                "Create Kafka assurance YAML for order lifecycle events",
                List.of(new AssuranceAssistantContextSource(
                        "asyncapi",
                        "order-events",
                        "ORDER_CREATED, PAYMENT_CAPTURED, ORDER_SHIPPED events with correlationId and schemaVersion"
                )),
                null,
                null,
                null
        ));

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.mode()).isEqualTo("local-template");
        assertThat(response.suggestedYaml())
                .contains("app:")
                .contains("metadata-rule")
                .contains("ordering-rules");
        assertThat(response.acceptedContextSources()).contains("asyncapi:order-events");
    }
}

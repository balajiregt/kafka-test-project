package com.example.kafkaeventlab.assurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
                false,
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
                false,
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

    @Test
    void includesConfiguredProjectContextWhenRequested() throws IOException {
        Path contextFile = Files.createTempFile("event-quality-testing-context", ".md");
        Files.writeString(contextFile, """
                # Event Quality Testing Context
                The billing application emits INVOICE_CREATED and PAYMENT_SETTLED Kafka events.
                """);

        AssuranceProperties properties = new AssuranceProperties();
        properties.getAssistant().setContextFiles(List.of(contextFile.toString()));
        AssuranceAssistantService service = new AssuranceAssistantService(properties, new ObjectMapper());

        AssuranceAssistantResponse response = service.advise(new AssuranceAssistantRequest(
                "Create Kafka assurance YAML from the project context",
                List.of(),
                true,
                null,
                null,
                null
        ));

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.acceptedContextSources())
                .contains("application-context:" + contextFile.getFileName());
        assertThat(response.warnings())
                .noneMatch(warning -> warning.contains("No context sources supplied"));
    }
}

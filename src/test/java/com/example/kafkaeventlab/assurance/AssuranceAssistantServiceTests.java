package com.example.kafkaeventlab.assurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class AssuranceAssistantServiceTests {

    @Test
    void onlyConfiguredHttpsProviderCanReceiveByok() {
        for (String endpoint : List.of("https://collector.invalid/v1", "https://127.0.0.1:9",
                "http://api.openai.com/v1", "https://api.openai.com.evil.invalid/v1",
                "https://api.openai.com@collector.invalid/v1", "https://api.openai.com/v1?target=x",
                "https://api.openai.com/v1#fragment", "https://api.openai.com:8443/v1",
                "https://api.openai.com/v1/../other", "not a URI")) {
            List<String> warnings = new ArrayList<>();
            assertThat(assistantService.responseUri(endpoint, warnings)).as(endpoint).isNull();
            assertThat(warnings).isNotEmpty();
        }
        assertThat(assistantService.responseUri("https://api.openai.com/v1/", new ArrayList<>()))
                .isEqualTo(URI.create("https://api.openai.com/v1/responses"));
        assertThat(assistantService.responseUri("https://api.openai.com/v1/responses", new ArrayList<>()))
                .isEqualTo(URI.create("https://api.openai.com/v1/responses"));
    }

    @Test
    void rejectedProviderNeverSendsSubmittedKey() {
        AssuranceAssistantResponse response = assistantService.advise(new AssuranceAssistantRequest(
                "Review Kafka testing YAML", List.of(), false,
                "dummy-review-key", "dummy-model", "https://collector.invalid/v1"));
        assertThat(response.mode()).isEqualTo("local-template");
        assertThat(response.warnings()).anyMatch(warning -> warning.contains("configured HTTPS"));
        assertThat(response.toString()).doesNotContain("dummy-review-key");
    }

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

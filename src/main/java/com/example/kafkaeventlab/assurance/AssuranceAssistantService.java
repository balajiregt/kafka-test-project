package com.example.kafkaeventlab.assurance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AssuranceAssistantService {

    private static final List<String> SCOPE_TERMS = List.of(
            "assurance", "quality", "qa", "testing", "yaml", "yml", "kafka", "event", "topic", "asyncapi", "openapi", "swagger",
            "schema", "contract", "metadata", "correlation", "aggregate", "subject", "offset",
            "testing", "test", "api", "integration", "e2e", "lifecycle", "graph", "rule",
            "assertion", "consumer", "producer", "dlq", "retry", "idempotency", "context",
            "capability", "glossary", "domain", "workflow", "journey"
    );

    private static final String SYSTEM_PROMPT = """
            You are an Event Quality Testing Config Assistant embedded in an event-driven QA/testing framework.
            You are restricted to helping users create, review, or explain app.assurance YAML, event metadata expectations,
            lifecycle ordering rules, graph edge mappings, context-source quality, and test-layer recommendations.
            Use only the supplied context, including project context files when present. If context is insufficient, say what context is missing.
            Refuse unrelated requests. Do not answer general coding, personal, news, finance, travel, or entertainment questions.
            Return concise output with:
            1. Recommended YAML
            2. Why these rules belong here
            3. Missing context or risks
            Never ask for secrets. Never persist or echo API keys.
            """;

    private final AssuranceProperties assuranceProperties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Autowired
    public AssuranceAssistantService(AssuranceProperties assuranceProperties, ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.assuranceProperties = assuranceProperties;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader == null ? new DefaultResourceLoader() : resourceLoader;
    }

    public AssuranceAssistantService(AssuranceProperties assuranceProperties, ObjectMapper objectMapper) {
        this(assuranceProperties, objectMapper, new DefaultResourceLoader());
    }

    public AssuranceAssistantResponse advise(AssuranceAssistantRequest request) {
        AssuranceProperties.Assistant assistant = assuranceProperties.getAssistant();
        if (!assistant.isEnabled()) {
            return response("DISABLED", "local", "The event quality testing assistant is disabled in configuration.", "", List.of(), List.of());
        }

        String message = clean(request == null ? null : request.message());
        List<String> warnings = new ArrayList<>();
        List<AssuranceAssistantContextSource> contextSources = acceptedContextSources(request, assistant, warnings);
        List<String> acceptedNames = contextSources.stream()
                .map(source -> "%s:%s".formatted(source.sourceType(), source.name()))
                .toList();

        if (!isInScope(message, contextSources)) {
            return response(
                    "REFUSED",
                    "guardrail",
                    "I can only help with event-driven QA/testing YAML, Kafka/event contracts, lifecycle rules, graph mappings, and test-layer strategy. Add AsyncAPI, OpenAPI, schema, workflow, sample payload, or testing context and ask a quality-testing configuration question.",
                    "",
                    warnings,
                    acceptedNames
            );
        }

        String apiKey = firstText(request == null ? null : request.apiKey(), environmentKey(assistant));
        String model = firstText(request == null ? null : request.model(), assistant.getDefaultModel());
        if (hasText(apiKey) && hasText(model)) {
            return aiResponse(request, contextSources, warnings, acceptedNames, apiKey, model, assistant);
        }

        if (!hasText(model) && hasText(apiKey)) {
            warnings.add("No model was supplied, so the assistant used deterministic local mode.");
        }
        return localResponse(message, contextSources, warnings, acceptedNames);
    }

    private AssuranceAssistantResponse aiResponse(
            AssuranceAssistantRequest request,
            List<AssuranceAssistantContextSource> contextSources,
            List<String> warnings,
            List<String> acceptedNames,
            String apiKey,
            String model,
            AssuranceProperties.Assistant assistant
    ) {
        URI uri = responseUri(firstText(request.baseUrl(), assistant.getDefaultBaseUrl()), warnings);
        if (uri == null) {
            return localResponse(request.message(), contextSources, warnings, acceptedNames);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt(request.message(), contextSources))
        ));
        body.put("temperature", 0.2);

        try {
            String raw = RestClient.builder()
                    .baseUrl(uri.toString())
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .post()
                    .body(body)
                    .retrieve()
                    .body(String.class);
            String reply = extractResponseText(raw);
            return response("OK", "byok-ai", reply, extractYaml(reply), warnings, acceptedNames);
        } catch (RestClientException | JsonProcessingException exception) {
            warnings.add("Live AI call failed, so deterministic local mode was used: " + exception.getClass().getSimpleName());
            return localResponse(request.message(), contextSources, warnings, acceptedNames);
        }
    }

    private AssuranceAssistantResponse localResponse(
            String message,
            List<AssuranceAssistantContextSource> contextSources,
            List<String> warnings,
            List<String> acceptedNames
    ) {
        List<String> contextTypes = contextSources.stream()
                .map(AssuranceAssistantContextSource::sourceType)
                .distinct()
                .toList();
        if (contextSources.isEmpty()) {
            warnings.add("No context sources supplied. The YAML below is a starter template, not project-specific output.");
        }
        if (contextTypes.stream().noneMatch(type -> type.equals("application-context"))) {
            warnings.add("Add or enable the project context Markdown file to capture domain vocabulary, lifecycles, and test boundaries.");
        }
        if (contextTypes.stream().noneMatch(type -> type.equals("asyncapi") || type.equals("event-catalog") || type.equals("schema"))) {
            warnings.add("Add AsyncAPI, event catalog, schema registry export, or sample payloads for stronger Kafka rules.");
        }
        if (contextTypes.stream().noneMatch(type -> type.equals("openapi") || type.equals("swagger") || type.equals("workflow"))) {
            warnings.add("Add OpenAPI/Swagger or workflow docs to connect API commands to lifecycle events.");
        }

        String yaml = starterYaml(contextSources);
        String reply = """
                I can help draft event quality testing configuration for this project, but this response is deterministic local mode because no BYOK model was supplied.

                Use the YAML as a starter, then replace placeholder event names with names from your AsyncAPI/event catalog and sample payloads.

                Recommended context inputs:
                - AsyncAPI or event catalog for Kafka contracts
                - OpenAPI/Swagger for command APIs
                - sample event payloads
                - consumer/topic ownership
                - workflow or lifecycle diagrams
                - existing API, Kafka, contract, and UI/e2e test strategy
                """;
        return response("OK", "local-template", reply, yaml, warnings, acceptedNames);
    }

    private List<AssuranceAssistantContextSource> acceptedContextSources(
            AssuranceAssistantRequest request,
            AssuranceProperties.Assistant assistant,
            List<String> warnings
    ) {
        List<AssuranceAssistantContextSource> candidates = new ArrayList<>();
        if (shouldIncludeProjectContext(request, assistant)) {
            candidates.addAll(configuredProjectContextSources(assistant, warnings));
        }
        if (request != null && request.contextSources() != null) {
            candidates.addAll(request.contextSources());
        }
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<AssuranceAssistantContextSource> accepted = new ArrayList<>();
        int totalCharacters = 0;
        for (AssuranceAssistantContextSource source : candidates) {
            if (accepted.size() >= assistant.getMaxContextSources()) {
                warnings.add("Some context sources were ignored because maxContextSources was reached.");
                break;
            }
            String sourceType = clean(source.sourceType()).toLowerCase(Locale.ROOT);
            if (!assistant.getAllowedContextTypes().contains(sourceType)) {
                warnings.add("Ignored unsupported context type: " + valueOrUnknown(source.sourceType()));
                continue;
            }
            String content = clean(source.content());
            if (!hasText(content)) {
                warnings.add("Ignored empty context source: " + valueOrUnknown(source.name()));
                continue;
            }
            int remaining = assistant.getMaxContextCharacters() - totalCharacters;
            if (remaining <= 0) {
                warnings.add("Some context was ignored because maxContextCharacters was reached.");
                break;
            }
            if (content.length() > remaining) {
                content = content.substring(0, remaining);
                warnings.add("Context was truncated to fit maxContextCharacters.");
            }
            totalCharacters += content.length();
            accepted.add(new AssuranceAssistantContextSource(
                    sourceType,
                    firstText(source.name(), "unnamed"),
                    content
            ));
        }
        return List.copyOf(accepted);
    }

    private boolean shouldIncludeProjectContext(AssuranceAssistantRequest request, AssuranceProperties.Assistant assistant) {
        if (request != null && request.includeProjectContext() != null) {
            return request.includeProjectContext();
        }
        return assistant.isIncludeContextFilesByDefault();
    }

    private List<AssuranceAssistantContextSource> configuredProjectContextSources(
            AssuranceProperties.Assistant assistant,
            List<String> warnings
    ) {
        if (assistant.getContextFiles() == null || assistant.getContextFiles().isEmpty()) {
            return List.of();
        }
        List<AssuranceAssistantContextSource> sources = new ArrayList<>();
        for (String location : assistant.getContextFiles()) {
            String content = readContextFile(location, warnings);
            if (hasText(content)) {
                sources.add(new AssuranceAssistantContextSource(
                        "application-context",
                        contextName(location),
                        content
                ));
            }
        }
        return List.copyOf(sources);
    }

    private String readContextFile(String location, List<String> warnings) {
        String cleanedLocation = clean(location);
        if (!hasText(cleanedLocation)) {
            return "";
        }
        try {
            if (cleanedLocation.startsWith("classpath:") || cleanedLocation.startsWith("file:")) {
                Resource resource = resourceLoader.getResource(cleanedLocation);
                if (resource.exists()) {
                    return resource.getContentAsString(StandardCharsets.UTF_8);
                }
                warnings.add("Configured project context file was not found: " + cleanedLocation);
                return "";
            }

            Path path = Path.of(cleanedLocation);
            if (!path.isAbsolute()) {
                path = Path.of(System.getProperty("user.dir")).resolve(path).normalize();
            }
            if (Files.exists(path)) {
                return Files.readString(path);
            }

            Resource resource = resourceLoader.getResource(cleanedLocation);
            if (resource.exists()) {
                return resource.getContentAsString(StandardCharsets.UTF_8);
            }
            warnings.add("Configured project context file was not found: " + cleanedLocation);
            return "";
        } catch (IOException exception) {
            warnings.add("Configured project context file could not be read: " + cleanedLocation);
            return "";
        }
    }

    private String contextName(String location) {
        String cleanedLocation = clean(location);
        int lastSlash = Math.max(cleanedLocation.lastIndexOf('/'), cleanedLocation.lastIndexOf('\\'));
        return lastSlash >= 0 ? cleanedLocation.substring(lastSlash + 1) : cleanedLocation;
    }

    private boolean isInScope(String message, List<AssuranceAssistantContextSource> contextSources) {
        String text = (message + " " + contextSources.stream()
                .map(source -> source.sourceType() + " " + source.name() + " " + source.content())
                .reduce("", (left, right) -> left + " " + right)).toLowerCase(Locale.ROOT);
        return SCOPE_TERMS.stream().anyMatch(text::contains);
    }

    private String userPrompt(String message, List<AssuranceAssistantContextSource> contextSources) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("User request:\n").append(message).append("\n\n");
        prompt.append("Context sources:\n");
        for (AssuranceAssistantContextSource source : contextSources) {
            prompt.append("\n--- ").append(source.sourceType()).append(" / ").append(source.name()).append(" ---\n")
                    .append(source.content()).append("\n");
        }
        prompt.append("\nGenerate or review app.assurance YAML only. Include missing context and test-layer guidance.");
        return prompt.toString();
    }

    private URI responseUri(String baseUrl, List<String> warnings) {
        if (!hasText(baseUrl)) {
            warnings.add("No base URL supplied for live AI.");
            return null;
        }
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String uri = normalized.endsWith("/responses") ? normalized : normalized + "/responses";
        if (!uri.startsWith("https://")) {
            warnings.add("Live AI base URL must use https, so deterministic local mode was used.");
            return null;
        }
        return URI.create(uri);
    }

    private String extractResponseText(String raw) throws JsonProcessingException {
        Map<String, Object> response = objectMapper.readValue(raw, new TypeReference<>() {
        });
        Object outputText = response.get("output_text");
        if (outputText instanceof String text && hasText(text)) {
            return text;
        }
        Object output = response.get("output");
        if (output instanceof List<?> items) {
            StringBuilder builder = new StringBuilder();
            for (Object item : items) {
                if (item instanceof Map<?, ?> itemMap && itemMap.get("content") instanceof List<?> contentItems) {
                    for (Object contentItem : contentItems) {
                        if (contentItem instanceof Map<?, ?> contentMap && contentMap.get("text") instanceof String text) {
                            builder.append(text).append("\n");
                        }
                    }
                }
            }
            if (!builder.isEmpty()) {
                return builder.toString().trim();
            }
        }
        return raw;
    }

    private String extractYaml(String reply) {
        if (!hasText(reply)) {
            return "";
        }
        int yamlStart = reply.indexOf("```yaml");
        if (yamlStart < 0) {
            yamlStart = reply.indexOf("```yml");
        }
        if (yamlStart < 0) {
            return reply.contains("app:") ? reply : "";
        }
        int bodyStart = reply.indexOf('\n', yamlStart);
        int bodyEnd = reply.indexOf("```", bodyStart + 1);
        if (bodyStart < 0 || bodyEnd < 0) {
            return "";
        }
        return reply.substring(bodyStart + 1, bodyEnd).trim();
    }

    private String starterYaml(List<AssuranceAssistantContextSource> contextSources) {
        String prefix = contextSources.isEmpty()
                ? "sample"
                : contextSources.get(0).name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (!hasText(prefix)) {
            prefix = "project";
        }
        return """
                app:
                  assurance:
                    metadata-rule:
                      enabled: true
                      rule-id: metadata.required-fields
                      required-fields:
                        - eventType
                        - aggregateId
                        - subjectId
                        - correlationId
                        - schemaVersion
                        - offsetNumber
                    ordering-rules:
                      - rule-id: %s.lifecycle.prerequisite-before-result
                        trigger-event-type: REPLACE_WITH_RESULT_EVENT
                        required-prior-event-type: REPLACE_WITH_PREREQUISITE_EVENT
                        severity-on-failure: HIGH
                        status-on-failure: FAIL
                        recommended-test-layer: Kafka integration
                    event-flag-rules:
                      - rule-id: %s.failures-reviewed
                        event-type: REPLACE_WITH_FAILURE_EVENT
                        severity-when-present: MEDIUM
                        status-when-present: WARN
                        recommended-test-layer: API integration
                    graph-edge-rules:
                      - when-aggregate-type: REPLACE_WITH_AGGREGATE_TYPE
                        edge-type: RELATES_TO_SUBJECT
                        label: "{eventType}"
                    testing-recommendations:
                      - capability: Event publication and consumption
                        test-layer: Kafka integration
                        why-here: Proves producer, topic, serializer, listener, offset, and evidence capture.
                        example-assertions:
                          - Expected event type is published for the command.
                          - Topic, partition, offset, correlation id, and schema version are captured.
                          - Events for the same subject keep expected ordering.
                """.formatted(prefix, prefix);
    }

    private String environmentKey(AssuranceProperties.Assistant assistant) {
        if (!hasText(assistant.getApiKeyEnv())) {
            return "";
        }
        return System.getenv(assistant.getApiKeyEnv());
    }

    private AssuranceAssistantResponse response(
            String status,
            String mode,
            String reply,
            String suggestedYaml,
            List<String> warnings,
            List<String> acceptedContextSources
    ) {
        return new AssuranceAssistantResponse(
                status,
                mode,
                reply,
                suggestedYaml,
                List.copyOf(warnings),
                List.copyOf(acceptedContextSources)
        );
    }

    private String firstText(String first, String second) {
        return hasText(first) ? first.trim() : clean(second);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String valueOrUnknown(String value) {
        return hasText(value) ? value : "unknown";
    }
}

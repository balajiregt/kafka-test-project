package com.example.kafkaeventlab.assurance;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.assurance")
public class AssuranceProperties {

    private EventPresenceRule eventPresenceRule = new EventPresenceRule();
    private MetadataRule metadataRule = new MetadataRule();
    private Evidence evidence = new Evidence();
    private Assistant assistant = new Assistant();
    private Playground playground = new Playground();
    private List<OrderingRule> orderingRules = new ArrayList<>();
    private List<EventFlagRule> eventFlagRules = new ArrayList<>();
    private List<GraphEdgeRule> graphEdgeRules = new ArrayList<>();
    private List<TestingRecommendationRule> testingRecommendations = new ArrayList<>();

    public EventPresenceRule getEventPresenceRule() {
        return eventPresenceRule;
    }

    public void setEventPresenceRule(EventPresenceRule eventPresenceRule) {
        this.eventPresenceRule = eventPresenceRule;
    }

    public MetadataRule getMetadataRule() {
        return metadataRule;
    }

    public void setMetadataRule(MetadataRule metadataRule) {
        this.metadataRule = metadataRule;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public Assistant getAssistant() {
        return assistant;
    }

    public void setAssistant(Assistant assistant) {
        this.assistant = assistant;
    }

    public Playground getPlayground() {
        return playground;
    }

    public void setPlayground(Playground playground) {
        this.playground = playground;
    }

    public List<OrderingRule> getOrderingRules() {
        return orderingRules;
    }

    public void setOrderingRules(List<OrderingRule> orderingRules) {
        this.orderingRules = orderingRules;
    }

    public List<EventFlagRule> getEventFlagRules() {
        return eventFlagRules;
    }

    public void setEventFlagRules(List<EventFlagRule> eventFlagRules) {
        this.eventFlagRules = eventFlagRules;
    }

    public List<GraphEdgeRule> getGraphEdgeRules() {
        return graphEdgeRules;
    }

    public void setGraphEdgeRules(List<GraphEdgeRule> graphEdgeRules) {
        this.graphEdgeRules = graphEdgeRules;
    }

    public List<TestingRecommendationRule> getTestingRecommendations() {
        return testingRecommendations;
    }

    public void setTestingRecommendations(List<TestingRecommendationRule> testingRecommendations) {
        this.testingRecommendations = testingRecommendations;
    }

    public static class EventPresenceRule {
        private boolean enabled = true;
        private String ruleId = "journey.events-present";
        private String emptySeverity = "HIGH";
        private String presentSeverity = "LOW";
        private String emptyStatus = "WARN";
        private String presentStatus = "PASS";
        private String emptyMessage = "No consumed events are available, so the advisor cannot prove this journey.";
        private String presentMessage = "Consumed events exist for this journey.";
        private String recommendedTestLayer = "Kafka integration";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }

        public String getEmptySeverity() {
            return emptySeverity;
        }

        public void setEmptySeverity(String emptySeverity) {
            this.emptySeverity = emptySeverity;
        }

        public String getPresentSeverity() {
            return presentSeverity;
        }

        public void setPresentSeverity(String presentSeverity) {
            this.presentSeverity = presentSeverity;
        }

        public String getEmptyStatus() {
            return emptyStatus;
        }

        public void setEmptyStatus(String emptyStatus) {
            this.emptyStatus = emptyStatus;
        }

        public String getPresentStatus() {
            return presentStatus;
        }

        public void setPresentStatus(String presentStatus) {
            this.presentStatus = presentStatus;
        }

        public String getEmptyMessage() {
            return emptyMessage;
        }

        public void setEmptyMessage(String emptyMessage) {
            this.emptyMessage = emptyMessage;
        }

        public String getPresentMessage() {
            return presentMessage;
        }

        public void setPresentMessage(String presentMessage) {
            this.presentMessage = presentMessage;
        }

        public String getRecommendedTestLayer() {
            return recommendedTestLayer;
        }

        public void setRecommendedTestLayer(String recommendedTestLayer) {
            this.recommendedTestLayer = recommendedTestLayer;
        }
    }

    public static class Evidence {
        private String sourceType = "jpa";

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }
    }

    public static class Assistant {
        private boolean enabled = true;
        private String apiKeyEnv = "OPENAI_API_KEY";
        private String defaultBaseUrl = "https://api.openai.com/v1";
        private String defaultModel = "";
        private int maxContextSources = 6;
        private int maxContextCharacters = 16000;
        private boolean includeContextFilesByDefault = true;
        private List<String> contextFiles = new ArrayList<>(List.of("docs/event-quality-testing-context.md"));
        private List<String> allowedContextTypes = new ArrayList<>(List.of(
                "application-context",
                "asyncapi",
                "openapi",
                "swagger",
                "event-catalog",
                "schema",
                "sample-payload",
                "workflow",
                "test-strategy",
                "consumer-ownership",
                "freeform"
        ));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKeyEnv() {
            return apiKeyEnv;
        }

        public void setApiKeyEnv(String apiKeyEnv) {
            this.apiKeyEnv = apiKeyEnv;
        }

        public String getDefaultBaseUrl() {
            return defaultBaseUrl;
        }

        public void setDefaultBaseUrl(String defaultBaseUrl) {
            this.defaultBaseUrl = defaultBaseUrl;
        }

        public String getDefaultModel() {
            return defaultModel;
        }

        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }

        public int getMaxContextSources() {
            return maxContextSources;
        }

        public void setMaxContextSources(int maxContextSources) {
            this.maxContextSources = maxContextSources;
        }

        public int getMaxContextCharacters() {
            return maxContextCharacters;
        }

        public void setMaxContextCharacters(int maxContextCharacters) {
            this.maxContextCharacters = maxContextCharacters;
        }

        public boolean isIncludeContextFilesByDefault() {
            return includeContextFilesByDefault;
        }

        public void setIncludeContextFilesByDefault(boolean includeContextFilesByDefault) {
            this.includeContextFilesByDefault = includeContextFilesByDefault;
        }

        public List<String> getContextFiles() {
            return contextFiles;
        }

        public void setContextFiles(List<String> contextFiles) {
            this.contextFiles = contextFiles;
        }

        public List<String> getAllowedContextTypes() {
            return allowedContextTypes;
        }

        public void setAllowedContextTypes(List<String> allowedContextTypes) {
            this.allowedContextTypes = allowedContextTypes;
        }
    }

    public static class Playground {
        private boolean enabled;
        private String defaultTopic = "playground.assurance.events.v1";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDefaultTopic() {
            return defaultTopic;
        }

        public void setDefaultTopic(String defaultTopic) {
            this.defaultTopic = defaultTopic;
        }
    }

    public static class MetadataRule {
        private boolean enabled = true;
        private String ruleId = "metadata.required-fields";
        private String severityOnMissing = "HIGH";
        private String severityOnPass = "LOW";
        private String statusOnMissing = "FAIL";
        private String statusOnPass = "PASS";
        private String missingMessage = "One or more events are missing required metadata fields: {requiredFields}.";
        private String passMessage = "All consumed events include required metadata fields: {requiredFields}.";
        private String recommendedTestLayer = "Contract/schema";
        private List<String> requiredFields = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }

        public String getSeverityOnMissing() {
            return severityOnMissing;
        }

        public void setSeverityOnMissing(String severityOnMissing) {
            this.severityOnMissing = severityOnMissing;
        }

        public String getSeverityOnPass() {
            return severityOnPass;
        }

        public void setSeverityOnPass(String severityOnPass) {
            this.severityOnPass = severityOnPass;
        }

        public String getStatusOnMissing() {
            return statusOnMissing;
        }

        public void setStatusOnMissing(String statusOnMissing) {
            this.statusOnMissing = statusOnMissing;
        }

        public String getStatusOnPass() {
            return statusOnPass;
        }

        public void setStatusOnPass(String statusOnPass) {
            this.statusOnPass = statusOnPass;
        }

        public String getMissingMessage() {
            return missingMessage;
        }

        public void setMissingMessage(String missingMessage) {
            this.missingMessage = missingMessage;
        }

        public String getPassMessage() {
            return passMessage;
        }

        public void setPassMessage(String passMessage) {
            this.passMessage = passMessage;
        }

        public String getRecommendedTestLayer() {
            return recommendedTestLayer;
        }

        public void setRecommendedTestLayer(String recommendedTestLayer) {
            this.recommendedTestLayer = recommendedTestLayer;
        }

        public List<String> getRequiredFields() {
            return requiredFields;
        }

        public void setRequiredFields(List<String> requiredFields) {
            this.requiredFields = requiredFields;
        }
    }

    public static class OrderingRule {
        private String ruleId;
        private String triggerEventType;
        private String requiredPriorEventType;
        private String severityOnFailure = "HIGH";
        private String severityOnPass = "LOW";
        private String statusOnFailure = "FAIL";
        private String statusOnPass = "PASS";
        private String statusWhenNotApplicable = "PASS";
        private String failureMessage = "{triggerEventType} was observed without prior {requiredPriorEventType}.";
        private String passMessage = "{requiredPriorEventType} was observed before {triggerEventType}.";
        private String notApplicableMessage = "{triggerEventType} was not observed, so this rule is not applicable.";
        private String recommendedTestLayer = "Kafka integration";

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }

        public String getTriggerEventType() {
            return triggerEventType;
        }

        public void setTriggerEventType(String triggerEventType) {
            this.triggerEventType = triggerEventType;
        }

        public String getRequiredPriorEventType() {
            return requiredPriorEventType;
        }

        public void setRequiredPriorEventType(String requiredPriorEventType) {
            this.requiredPriorEventType = requiredPriorEventType;
        }

        public String getSeverityOnFailure() {
            return severityOnFailure;
        }

        public void setSeverityOnFailure(String severityOnFailure) {
            this.severityOnFailure = severityOnFailure;
        }

        public String getSeverityOnPass() {
            return severityOnPass;
        }

        public void setSeverityOnPass(String severityOnPass) {
            this.severityOnPass = severityOnPass;
        }

        public String getStatusOnFailure() {
            return statusOnFailure;
        }

        public void setStatusOnFailure(String statusOnFailure) {
            this.statusOnFailure = statusOnFailure;
        }

        public String getStatusOnPass() {
            return statusOnPass;
        }

        public void setStatusOnPass(String statusOnPass) {
            this.statusOnPass = statusOnPass;
        }

        public String getStatusWhenNotApplicable() {
            return statusWhenNotApplicable;
        }

        public void setStatusWhenNotApplicable(String statusWhenNotApplicable) {
            this.statusWhenNotApplicable = statusWhenNotApplicable;
        }

        public String getFailureMessage() {
            return failureMessage;
        }

        public void setFailureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
        }

        public String getPassMessage() {
            return passMessage;
        }

        public void setPassMessage(String passMessage) {
            this.passMessage = passMessage;
        }

        public String getNotApplicableMessage() {
            return notApplicableMessage;
        }

        public void setNotApplicableMessage(String notApplicableMessage) {
            this.notApplicableMessage = notApplicableMessage;
        }

        public String getRecommendedTestLayer() {
            return recommendedTestLayer;
        }

        public void setRecommendedTestLayer(String recommendedTestLayer) {
            this.recommendedTestLayer = recommendedTestLayer;
        }
    }

    public static class EventFlagRule {
        private String ruleId;
        private String eventType;
        private String severityWhenPresent = "MEDIUM";
        private String severityWhenAbsent = "LOW";
        private String statusWhenPresent = "WARN";
        private String statusWhenAbsent = "PASS";
        private String presentMessage = "{eventType} events are present.";
        private String absentMessage = "No {eventType} events were observed.";
        private String recommendedTestLayer = "API integration";

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getSeverityWhenPresent() {
            return severityWhenPresent;
        }

        public void setSeverityWhenPresent(String severityWhenPresent) {
            this.severityWhenPresent = severityWhenPresent;
        }

        public String getSeverityWhenAbsent() {
            return severityWhenAbsent;
        }

        public void setSeverityWhenAbsent(String severityWhenAbsent) {
            this.severityWhenAbsent = severityWhenAbsent;
        }

        public String getStatusWhenPresent() {
            return statusWhenPresent;
        }

        public void setStatusWhenPresent(String statusWhenPresent) {
            this.statusWhenPresent = statusWhenPresent;
        }

        public String getStatusWhenAbsent() {
            return statusWhenAbsent;
        }

        public void setStatusWhenAbsent(String statusWhenAbsent) {
            this.statusWhenAbsent = statusWhenAbsent;
        }

        public String getPresentMessage() {
            return presentMessage;
        }

        public void setPresentMessage(String presentMessage) {
            this.presentMessage = presentMessage;
        }

        public String getAbsentMessage() {
            return absentMessage;
        }

        public void setAbsentMessage(String absentMessage) {
            this.absentMessage = absentMessage;
        }

        public String getRecommendedTestLayer() {
            return recommendedTestLayer;
        }

        public void setRecommendedTestLayer(String recommendedTestLayer) {
            this.recommendedTestLayer = recommendedTestLayer;
        }
    }

    public static class GraphEdgeRule {
        private String whenAggregateType;
        private String edgeType;
        private String label = "{eventType}";

        public String getWhenAggregateType() {
            return whenAggregateType;
        }

        public void setWhenAggregateType(String whenAggregateType) {
            this.whenAggregateType = whenAggregateType;
        }

        public String getEdgeType() {
            return edgeType;
        }

        public void setEdgeType(String edgeType) {
            this.edgeType = edgeType;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class TestingRecommendationRule {
        private String capability;
        private String testLayer;
        private String whyHere;
        private List<String> exampleAssertions = new ArrayList<>();

        public String getCapability() {
            return capability;
        }

        public void setCapability(String capability) {
            this.capability = capability;
        }

        public String getTestLayer() {
            return testLayer;
        }

        public void setTestLayer(String testLayer) {
            this.testLayer = testLayer;
        }

        public String getWhyHere() {
            return whyHere;
        }

        public void setWhyHere(String whyHere) {
            this.whyHere = whyHere;
        }

        public List<String> getExampleAssertions() {
            return exampleAssertions;
        }

        public void setExampleAssertions(List<String> exampleAssertions) {
            this.exampleAssertions = exampleAssertions;
        }
    }
}

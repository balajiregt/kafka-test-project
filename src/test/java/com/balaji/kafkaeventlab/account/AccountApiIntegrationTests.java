package com.balaji.kafkaeventlab.account;

import com.balaji.kafkaeventlab.account.domain.CredentialType;
import com.balaji.kafkaeventlab.account.domain.PasswordResetRequestRepository;
import com.balaji.kafkaeventlab.account.domain.SignInAttemptRepository;
import com.balaji.kafkaeventlab.account.domain.UserCredentialRepository;
import com.balaji.kafkaeventlab.account.domain.UserProfileRepository;
import com.balaji.kafkaeventlab.account.domain.UserStatus;
import com.balaji.kafkaeventlab.account.event.AccountEvent;
import com.balaji.kafkaeventlab.account.event.AccountEventPublisher;
import com.balaji.kafkaeventlab.assurance.EventEvidence;
import com.balaji.kafkaeventlab.assurance.EventEvidenceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.CompletableFuture;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false",
        "app.assurance.playground.enabled=true"
})
@AutoConfigureMockMvc
class AccountApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private SignInAttemptRepository signInAttemptRepository;

    @Autowired
    private PasswordResetRequestRepository passwordResetRequestRepository;

    @Autowired
    private EventEvidenceRepository eventEvidenceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountEventPublisher eventPublisher;

    private long offsetSequence;

    @BeforeEach
    void setUp() {
        eventEvidenceRepository.deleteAll();
        passwordResetRequestRepository.deleteAll();
        signInAttemptRepository.deleteAll();
        userCredentialRepository.deleteAll();
        userProfileRepository.deleteAll();
        offsetSequence = 0;
        when(eventPublisher.publish(any(AccountEvent.class)))
                .thenAnswer(invocation -> {
                    AccountEvent event = invocation.getArgument(0);
                    eventEvidenceRepository.save(new EventEvidence(
                            event.eventId(),
                            event.eventType(),
                            event.aggregateType(),
                            event.aggregateId(),
                            event.subjectUserId(),
                            event.correlationId(),
                            "account.events.v1",
                            0,
                            offsetSequence++,
                            event.occurredAt(),
                            event.occurredAt(),
                            event.schemaVersion(),
                            objectMapper.writeValueAsString(event)
                    ));
                    return CompletableFuture.completedFuture(event);
                });
    }

    @Test
    void supportsCoreAccountProfileAndAuthenticationLifecycle() throws Exception {
        performAsyncPost("/api/accounts/users", """
                {
                  "email": "mira.shah@example.com",
                  "displayName": "Mira Shah",
                  "department": "Security",
                  "jobTitle": "Profile Support Analyst"
                }
                """)
                .andExpect(jsonPath("$.eventType").value("USER_REGISTERED"))
                .andExpect(jsonPath("$.data.email").value("mira.shah@example.com"));

        var user = userProfileRepository.findByEmail("mira.shah@example.com").orElseThrow();
        assertThat(user.getDepartment()).isEqualTo("Security");

        performAsyncPost("/api/accounts/users/" + user.getUserId() + "/status", """
                {
                  "status": "ACTIVE",
                  "reason": "HR onboarding completed"
                }
                """)
                .andExpect(jsonPath("$.eventType").value("USER_STATUS_CHANGED"));

        performAsyncPost("/api/accounts/credentials", """
                {
                  "userId": "%s",
                  "credentialType": "PASSWORD_HASH",
                  "credentialReference": "password-hash:v1:mira",
                  "mfaEnabled": true
                }
                """.formatted(user.getUserId()))
                .andExpect(jsonPath("$.eventType").value("CREDENTIAL_REGISTERED"));

        performAsyncPost("/api/accounts/sign-ins", """
                {
                  "email": "mira.shah@example.com",
                  "credentialReference": "password-hash:v1:mira"
                }
                """)
                .andExpect(jsonPath("$.eventType").value("SIGN_IN_SUCCEEDED"));

        performAsyncPost("/api/accounts/password-reset-requests", """
                {
                  "email": "mira.shah@example.com"
                }
                """)
                .andExpect(jsonPath("$.eventType").value("PASSWORD_RESET_REQUESTED"));

        var resetRequest = passwordResetRequestRepository.findByUserId(user.getUserId()).get(0);
        performAsyncPost("/api/accounts/password-resets/complete", """
                {
                  "resetRequestId": "%s",
                  "newCredentialReference": "password-hash:v2:mira",
                  "mfaEnabled": true
                }
                """.formatted(resetRequest.getResetRequestId()))
                .andExpect(jsonPath("$.eventType").value("PASSWORD_RESET_COMPLETED"));

        assertThat(userProfileRepository.findById(user.getUserId()))
                .isPresent()
                .get()
                .satisfies(profile -> assertThat(profile.getStatus()).isEqualTo(UserStatus.ACTIVE));
        assertThat(userCredentialRepository.findByUserId(user.getUserId()))
                .anySatisfy(credential -> {
                    assertThat(credential.getCredentialType()).isEqualTo(CredentialType.PASSWORD_HASH);
                    assertThat(credential.getCredentialReference()).isEqualTo("password-hash:v2:mira");
                });
        assertThat(signInAttemptRepository.findByUserId(user.getUserId()))
                .hasSize(1)
                .first()
                .satisfies(attempt -> assertThat(attempt.getReason()).isEqualTo("Credential accepted"));

        mockMvc.perform(get("/api/assurance/subjects/" + user.getUserId() + "/journey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalEvents").value(6))
                .andExpect(jsonPath("$.summary.graphMetrics.eventNodeCount").value(6))
                .andExpect(jsonPath("$.summary.graphMetrics.metadataCompleteEvents").value(6))
                .andExpect(jsonPath("$.summary.graphMetrics.missingMetadataEvents").value(0))
                .andExpect(jsonPath("$.summary.graphMetrics.sequenceEdges").value(5))
                .andExpect(jsonPath("$.orderedEvents[0].eventType").value("USER_REGISTERED"))
                .andExpect(jsonPath("$.edges[?(@.edgeType == 'FOLLOWS_EVENT')]").isNotEmpty());

        mockMvc.perform(get("/api/assurance/subjects/" + user.getUserId() + "/findings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].ruleId").value("metadata.required-fields"))
                .andExpect(jsonPath("$[1].status").value("PASS"))
                .andExpect(jsonPath("$[3].ruleId").value("auth.credential-before-successful-sign-in"))
                .andExpect(jsonPath("$[3].status").value("PASS"))
                .andExpect(jsonPath("$[?(@.status == 'FAIL')]").isEmpty());

        mockMvc.perform(get("/api/assurance/testing-recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].testLayer").value("API integration"))
                .andExpect(jsonPath("$[?(@.testLayer == 'Kafka integration')]").isNotEmpty());

        mockMvc.perform(get("/assurance-report.html").param("subjectId", user.getUserId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Event Assurance Report")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Metadata Complete")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Click a graph node")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Assurance Config Assistant")));
    }

    @Test
    void evaluatesUserSuppliedPlaygroundEvidence() throws Exception {
        UUID sequenceSubject = UUID.randomUUID();
        mockMvc.perform(post("/api/assurance/playground/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioName": "sequence-gap",
                                  "subjectId": "%s",
                                  "events": [
                                    {
                                      "eventType": "USER_REGISTERED",
                                      "aggregateType": "USER",
                                      "aggregateId": "%s",
                                      "subjectId": "%s",
                                      "correlationId": "corr-sequence-001",
                                      "topicName": "playground.assurance.events.v1",
                                      "partitionNumber": 0,
                                      "offsetNumber": 0,
                                      "schemaVersion": 1,
                                      "data": {"status": "ACTIVE"}
                                    },
                                    {
                                      "eventType": "SIGN_IN_SUCCEEDED",
                                      "aggregateType": "SIGN_IN_ATTEMPT",
                                      "aggregateId": "%s",
                                      "subjectId": "%s",
                                      "correlationId": "corr-sequence-002",
                                      "topicName": "playground.assurance.events.v1",
                                      "partitionNumber": 0,
                                      "offsetNumber": 1,
                                      "schemaVersion": 1,
                                      "data": {"result": "SUCCESS"}
                                    }
                                  ]
                                }
                                """.formatted(
                                sequenceSubject,
                                sequenceSubject,
                                sequenceSubject,
                                UUID.randomUUID(),
                                sequenceSubject
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalEvents").value(2))
                .andExpect(jsonPath("$.findings[?(@.ruleId == 'auth.credential-before-successful-sign-in' && @.status == 'FAIL')]").isNotEmpty())
                .andExpect(jsonPath("$.reportPath").value(org.hamcrest.Matchers.startsWith("/assurance-report.html?subjectId=")));

        UUID metadataSubject = UUID.randomUUID();
        mockMvc.perform(post("/api/assurance/playground/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioName": "metadata-gap",
                                  "subjectId": "%s",
                                  "events": [
                                    {
                                      "eventType": "USER_REGISTERED",
                                      "aggregateType": "USER",
                                      "subjectId": "%s",
                                      "topicName": "playground.assurance.events.v1",
                                      "partitionNumber": 0,
                                      "data": {"status": "PENDING_ACTIVATION"}
                                    }
                                  ]
                                }
                                """.formatted(metadataSubject, metadataSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.graphMetrics.missingMetadataEvents").value(1))
                .andExpect(jsonPath("$.findings[?(@.ruleId == 'metadata.required-fields' && @.status == 'FAIL')]").isNotEmpty());

        UUID resetSubject = UUID.randomUUID();
        mockMvc.perform(post("/api/assurance/playground/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioName": "reset-gap",
                                  "subjectId": "%s",
                                  "events": [
                                    {
                                      "eventType": "USER_REGISTERED",
                                      "aggregateType": "USER",
                                      "aggregateId": "%s",
                                      "subjectId": "%s",
                                      "correlationId": "corr-reset-001",
                                      "topicName": "playground.assurance.events.v1",
                                      "partitionNumber": 0,
                                      "offsetNumber": 0,
                                      "schemaVersion": 1,
                                      "data": {"status": "ACTIVE"}
                                    },
                                    {
                                      "eventType": "PASSWORD_RESET_COMPLETED",
                                      "aggregateType": "PASSWORD_RESET",
                                      "aggregateId": "%s",
                                      "subjectId": "%s",
                                      "correlationId": "corr-reset-002",
                                      "topicName": "playground.assurance.events.v1",
                                      "partitionNumber": 0,
                                      "offsetNumber": 1,
                                      "schemaVersion": 1,
                                      "data": {"status": "COMPLETED"}
                                    }
                                  ]
                                }
                                """.formatted(
                                resetSubject,
                                resetSubject,
                                resetSubject,
                                UUID.randomUUID(),
                                resetSubject
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[?(@.ruleId == 'reset.request-before-completion' && @.status == 'FAIL')]").isNotEmpty());

        UUID warningSubject = UUID.randomUUID();
        mockMvc.perform(post("/api/assurance/playground/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenarioName": "warning-event",
                                  "subjectId": "%s",
                                  "events": [
                                    {
                                      "eventType": "USER_REGISTERED",
                                      "aggregateType": "USER",
                                      "aggregateId": "%s",
                                      "subjectId": "%s",
                                      "correlationId": "corr-warning-001",
                                      "topicName": "playground.assurance.events.v1",
                                      "partitionNumber": 0,
                                      "offsetNumber": 0,
                                      "schemaVersion": 1,
                                      "data": {"status": "ACTIVE"}
                                    },
                                    {
                                      "eventType": "SIGN_IN_FAILED",
                                      "aggregateType": "SIGN_IN_ATTEMPT",
                                      "aggregateId": "%s",
                                      "subjectId": "%s",
                                      "correlationId": "corr-warning-002",
                                      "topicName": "playground.assurance.events.v1",
                                      "partitionNumber": 0,
                                      "offsetNumber": 1,
                                      "schemaVersion": 1,
                                      "data": {"result": "FAILURE", "reason": "Credential was not accepted"}
                                    }
                                  ]
                                }
                                """.formatted(
                                warningSubject,
                                warningSubject,
                                warningSubject,
                                UUID.randomUUID(),
                                warningSubject
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[?(@.ruleId == 'auth.failed-sign-ins-explained' && @.status == 'WARN')]").isNotEmpty());
    }

    private ResultActions performAsyncPost(String url, String json) throws Exception {
        MvcResult result = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted())
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isAccepted());
    }
}

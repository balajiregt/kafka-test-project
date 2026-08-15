package com.balaji.kafkaeventlab.account;

import com.balaji.kafkaeventlab.account.application.CompletePasswordResetCommand;
import com.balaji.kafkaeventlab.account.application.AccountCommandService;
import com.balaji.kafkaeventlab.account.application.RegisterCredentialCommand;
import com.balaji.kafkaeventlab.account.application.RegisterUserCommand;
import com.balaji.kafkaeventlab.account.application.RequestPasswordResetCommand;
import com.balaji.kafkaeventlab.account.application.SignInCommand;
import com.balaji.kafkaeventlab.account.application.UpdateUserStatusCommand;
import com.balaji.kafkaeventlab.assurance.EventAssuranceAdvisor;
import com.balaji.kafkaeventlab.assurance.EventAssuranceGraphService;
import com.balaji.kafkaeventlab.account.domain.CredentialType;
import com.balaji.kafkaeventlab.account.domain.UserStatus;
import com.balaji.kafkaeventlab.account.event.AccountEvent;
import com.balaji.kafkaeventlab.assurance.EventEvidenceRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AccountKafkaIntegrationTests {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "account-event-tracker-test");
    }

    @Autowired
    private AccountCommandService commandService;

    @Autowired
    private EventEvidenceRepository eventEvidenceRepository;

    @Autowired
    private EventAssuranceGraphService graphService;

    @Autowired
    private EventAssuranceAdvisor advisor;

    @Test
    void publishesConsumesAndTracksAccountProfileAndAuthenticationLifecycle() {
        AccountEvent userRegistered = commandService.registerUser(new RegisterUserCommand(
                "anika.rao@example.com",
                "Anika Rao",
                "Finance",
                "Controller"
        )).join();

        AccountEvent userActivated = commandService.updateUserStatus(
                userRegistered.aggregateId(),
                new UpdateUserStatusCommand(UserStatus.ACTIVE, "HR onboarding completed")
        ).join();

        AccountEvent credentialRegistered = commandService.registerCredential(new RegisterCredentialCommand(
                userRegistered.aggregateId(),
                CredentialType.PASSWORD_HASH,
                "password-hash:v1:anika",
                true
        )).join();

        AccountEvent signInSucceeded = commandService.signIn(new SignInCommand(
                "anika.rao@example.com",
                "password-hash:v1:anika"
        )).join();

        AccountEvent resetRequested = commandService.requestPasswordReset(new RequestPasswordResetCommand(
                "anika.rao@example.com"
        )).join();

        AccountEvent resetCompleted = commandService.completePasswordReset(new CompletePasswordResetCommand(
                resetRequested.aggregateId(),
                "password-hash:v2:anika",
                true
        )).join();

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    assertThat(eventEvidenceRepository.findById(userRegistered.eventId())).isPresent();
                    assertThat(eventEvidenceRepository.findById(userActivated.eventId())).isPresent();
                    assertThat(eventEvidenceRepository.findById(credentialRegistered.eventId())).isPresent();
                    assertThat(eventEvidenceRepository.findById(signInSucceeded.eventId()))
                            .isPresent()
                            .get()
                            .satisfies(event -> assertThat(event.getEventType()).isEqualTo("SIGN_IN_SUCCEEDED"));
                    assertThat(eventEvidenceRepository.findById(resetRequested.eventId())).isPresent();
                    assertThat(eventEvidenceRepository.findById(resetCompleted.eventId()))
                            .isPresent()
                            .get()
                            .satisfies(event -> assertThat(event.getAggregateId()).isEqualTo(resetRequested.aggregateId()));
                });

        var journey = graphService.journeyForSubject(userRegistered.aggregateId());
        assertThat(journey.summary().totalEvents()).isEqualTo(6);
        assertThat(journey.summary().graphMetrics().eventNodeCount()).isEqualTo(6);
        assertThat(journey.summary().graphMetrics().metadataCompleteEvents()).isEqualTo(6);
        assertThat(journey.summary().graphMetrics().sequenceEdges()).isEqualTo(5);
        assertThat(journey.summary().graphMetrics().mostConnectedNodeDegree()).isGreaterThan(0);
        assertThat(journey.edges())
                .anySatisfy(edge -> assertThat(edge.edgeType()).isEqualTo("FOLLOWS_EVENT"))
                .anySatisfy(edge -> assertThat(edge.edgeType()).isEqualTo("AUTHENTICATES_WITH"))
                .anySatisfy(edge -> assertThat(edge.edgeType()).isEqualTo("RESETS_CREDENTIAL"));
        assertThat(advisor.findings(journey))
                .extracting(finding -> finding.status())
                .containsOnly("PASS");
    }
}

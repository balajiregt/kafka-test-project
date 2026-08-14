package com.balaji.kafkaeventlab.identity;

import com.balaji.kafkaeventlab.identity.application.CreateApplicationCommand;
import com.balaji.kafkaeventlab.identity.application.GrantApplicationAccessCommand;
import com.balaji.kafkaeventlab.identity.application.IdentityCommandService;
import com.balaji.kafkaeventlab.identity.application.RegisterUserCommand;
import com.balaji.kafkaeventlab.identity.application.UpdateUserStatusCommand;
import com.balaji.kafkaeventlab.identity.domain.UserStatus;
import com.balaji.kafkaeventlab.identity.event.IdentityEvent;
import com.balaji.kafkaeventlab.identity.projection.UserAccessSummaryRepository;
import com.balaji.kafkaeventlab.identity.tracking.IdentityTrackedEventRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class IdentityPlatformKafkaIntegrationTests {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> "identity-platform-read-model-test");
    }

    @Autowired
    private IdentityCommandService commandService;

    @Autowired
    private IdentityTrackedEventRepository trackedEventRepository;

    @Autowired
    private UserAccessSummaryRepository accessSummaryRepository;

    @Test
    void publishesConsumesTracksAndProjectsIdentityAccessLifecycle() {
        IdentityEvent userRegistered = commandService.registerUser(new RegisterUserCommand(
                "anika.rao@example.com",
                "Anika Rao",
                "Finance",
                "Controller"
        )).join();

        IdentityEvent applicationCreated = commandService.createApplication(new CreateApplicationCommand(
                "ERP",
                "Enterprise Resource Planning",
                "enterprise-platform",
                Set.of("APPROVER", "VIEWER")
        )).join();

        commandService.updateUserStatus(
                userRegistered.aggregateId(),
                new UpdateUserStatusCommand(UserStatus.ACTIVE, "HR onboarding completed")
        ).join();

        IdentityEvent accessGranted = commandService.grantAccess(new GrantApplicationAccessCommand(
                userRegistered.aggregateId(),
                applicationCreated.aggregateId(),
                "APPROVER",
                "iam-admin",
                null
        )).join();

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    assertThat(trackedEventRepository.findById(userRegistered.eventId())).isPresent();
                    assertThat(trackedEventRepository.findById(applicationCreated.eventId())).isPresent();
                    assertThat(trackedEventRepository.findById(accessGranted.eventId())).isPresent();

                    assertThat(accessSummaryRepository.findById(accessGranted.aggregateId()))
                            .isPresent()
                            .get()
                            .satisfies(summary -> {
                                assertThat(summary.getUserId()).isEqualTo(userRegistered.aggregateId());
                                assertThat(summary.getApplicationId()).isEqualTo(applicationCreated.aggregateId());
                                assertThat(summary.getApplicationCode()).isEqualTo("ERP");
                                assertThat(summary.getRoleName()).isEqualTo("APPROVER");
                                assertThat(summary.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
                            });
                });
    }
}

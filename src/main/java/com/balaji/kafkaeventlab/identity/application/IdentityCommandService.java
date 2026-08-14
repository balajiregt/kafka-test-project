package com.balaji.kafkaeventlab.identity.application;

import com.balaji.kafkaeventlab.identity.domain.ApplicationAccessGrant;
import com.balaji.kafkaeventlab.identity.domain.ApplicationAccessGrantRepository;
import com.balaji.kafkaeventlab.identity.domain.IdentityApplication;
import com.balaji.kafkaeventlab.identity.domain.IdentityApplicationRepository;
import com.balaji.kafkaeventlab.identity.domain.UserCredential;
import com.balaji.kafkaeventlab.identity.domain.UserCredentialRepository;
import com.balaji.kafkaeventlab.identity.domain.UserProfile;
import com.balaji.kafkaeventlab.identity.domain.UserProfileRepository;
import com.balaji.kafkaeventlab.identity.event.IdentityEvent;
import com.balaji.kafkaeventlab.identity.event.IdentityEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class IdentityCommandService {

    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final IdentityApplicationRepository identityApplicationRepository;
    private final ApplicationAccessGrantRepository accessGrantRepository;
    private final IdentityEventPublisher eventPublisher;

    public IdentityCommandService(
            UserProfileRepository userProfileRepository,
            UserCredentialRepository userCredentialRepository,
            IdentityApplicationRepository identityApplicationRepository,
            ApplicationAccessGrantRepository accessGrantRepository,
            IdentityEventPublisher eventPublisher
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.identityApplicationRepository = identityApplicationRepository;
        this.accessGrantRepository = accessGrantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CompletableFuture<IdentityEvent> registerUser(RegisterUserCommand command) {
        userProfileRepository.findByEmail(command.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("User email already exists: " + existing.getEmail());
        });

        UserProfile user = userProfileRepository.save(UserProfile.register(
                command.email(),
                command.displayName(),
                command.department(),
                command.jobTitle()
        ));

        return eventPublisher.publish(IdentityEvent.userRegistered(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getDepartment(),
                user.getJobTitle(),
                user.getStatus().name()
        ));
    }

    @Transactional
    public CompletableFuture<IdentityEvent> updateUserStatus(UUID userId, UpdateUserStatusCommand command) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.changeStatus(command.status());
        userProfileRepository.save(user);

        return eventPublisher.publish(IdentityEvent.userStatusChanged(
                user.getUserId(),
                user.getStatus().name(),
                command.reason()
        ));
    }

    @Transactional
    public CompletableFuture<IdentityEvent> registerCredential(RegisterCredentialCommand command) {
        userProfileRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        UserCredential credential = userCredentialRepository.save(UserCredential.register(
                command.userId(),
                command.credentialType(),
                command.credentialReference(),
                command.mfaEnabled()
        ));

        return eventPublisher.publish(IdentityEvent.credentialRegistered(
                credential.getCredentialId(),
                credential.getUserId(),
                credential.getCredentialType().name(),
                credential.getCredentialReference(),
                credential.getStatus().name(),
                credential.isMfaEnabled()
        ));
    }

    @Transactional
    public CompletableFuture<IdentityEvent> createApplication(CreateApplicationCommand command) {
        identityApplicationRepository.findByApplicationCode(command.applicationCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Application code already exists: " + existing.getApplicationCode());
        });

        IdentityApplication application = identityApplicationRepository.save(IdentityApplication.create(
                command.applicationCode(),
                command.displayName(),
                command.ownerTeam(),
                command.defaultRoles()
        ));

        return eventPublisher.publish(IdentityEvent.applicationCreated(
                application.getApplicationId(),
                application.getApplicationCode(),
                application.getDisplayName(),
                application.getOwnerTeam(),
                application.getStatus().name()
        ));
    }

    @Transactional
    public CompletableFuture<IdentityEvent> grantAccess(GrantApplicationAccessCommand command) {
        UserProfile user = userProfileRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));
        IdentityApplication application = identityApplicationRepository.findById(command.applicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + command.applicationId()));

        ApplicationAccessGrant grant = accessGrantRepository.save(ApplicationAccessGrant.grant(
                command.userId(),
                command.applicationId(),
                command.roleName(),
                command.grantedBy(),
                command.expiresAt()
        ));

        return eventPublisher.publish(IdentityEvent.accessGranted(
                grant.getAccessId(),
                user.getUserId(),
                application.getApplicationId(),
                user.getEmail(),
                user.getStatus().name(),
                application.getApplicationCode(),
                application.getStatus().name(),
                grant.getRoleName(),
                grant.getGrantedBy()
        ));
    }

    @Transactional
    public CompletableFuture<IdentityEvent> revokeAccess(RevokeApplicationAccessCommand command) {
        ApplicationAccessGrant grant = accessGrantRepository.findById(command.accessId())
                .orElseThrow(() -> new IllegalArgumentException("Access grant not found: " + command.accessId()));
        grant.revoke(command.revokedBy(), command.reason());
        accessGrantRepository.save(grant);

        return eventPublisher.publish(IdentityEvent.accessRevoked(
                grant.getAccessId(),
                grant.getUserId(),
                grant.getApplicationId(),
                grant.getRevokedBy(),
                grant.getRevocationReason()
        ));
    }
}

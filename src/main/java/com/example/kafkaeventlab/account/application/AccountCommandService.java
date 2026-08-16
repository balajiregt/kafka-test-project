package com.example.kafkaeventlab.account.application;

import com.example.kafkaeventlab.account.domain.AuthenticationResult;
import com.example.kafkaeventlab.account.domain.CredentialStatus;
import com.example.kafkaeventlab.account.domain.CredentialType;
import com.example.kafkaeventlab.account.domain.PasswordResetRequest;
import com.example.kafkaeventlab.account.domain.PasswordResetRequestRepository;
import com.example.kafkaeventlab.account.domain.PasswordResetStatus;
import com.example.kafkaeventlab.account.domain.SignInAttempt;
import com.example.kafkaeventlab.account.domain.SignInAttemptRepository;
import com.example.kafkaeventlab.account.domain.UserCredential;
import com.example.kafkaeventlab.account.domain.UserCredentialRepository;
import com.example.kafkaeventlab.account.domain.UserProfile;
import com.example.kafkaeventlab.account.domain.UserProfileRepository;
import com.example.kafkaeventlab.account.domain.UserStatus;
import com.example.kafkaeventlab.account.event.AccountEvent;
import com.example.kafkaeventlab.account.event.AccountEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AccountCommandService {

    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final SignInAttemptRepository signInAttemptRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final AccountEventPublisher eventPublisher;

    public AccountCommandService(
            UserProfileRepository userProfileRepository,
            UserCredentialRepository userCredentialRepository,
            SignInAttemptRepository signInAttemptRepository,
            PasswordResetRequestRepository passwordResetRequestRepository,
            AccountEventPublisher eventPublisher
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.signInAttemptRepository = signInAttemptRepository;
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CompletableFuture<AccountEvent> registerUser(RegisterUserCommand command) {
        userProfileRepository.findByEmail(command.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("User email already exists: " + existing.getEmail());
        });

        UserProfile user = userProfileRepository.save(UserProfile.register(
                command.email(),
                command.displayName(),
                command.department(),
                command.jobTitle()
        ));

        return eventPublisher.publish(AccountEvent.userRegistered(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getDepartment(),
                user.getJobTitle(),
                user.getStatus().name()
        ));
    }

    @Transactional
    public CompletableFuture<AccountEvent> updateUserStatus(UUID userId, UpdateUserStatusCommand command) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.changeStatus(command.status());
        userProfileRepository.save(user);

        return eventPublisher.publish(AccountEvent.userStatusChanged(
                user.getUserId(),
                user.getStatus().name(),
                command.reason()
        ));
    }

    @Transactional
    public CompletableFuture<AccountEvent> updateUserProfile(UUID userId, UpdateUserProfileCommand command) {
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.updateProfile(command.displayName(), command.department(), command.jobTitle());
        userProfileRepository.save(user);

        return eventPublisher.publish(AccountEvent.userProfileUpdated(
                user.getUserId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getDepartment(),
                user.getJobTitle()
        ));
    }

    @Transactional
    public CompletableFuture<AccountEvent> registerCredential(RegisterCredentialCommand command) {
        userProfileRepository.findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        UserCredential credential = userCredentialRepository.save(UserCredential.register(
                command.userId(),
                command.credentialType(),
                command.credentialReference(),
                command.mfaEnabled()
        ));

        return eventPublisher.publish(AccountEvent.credentialRegistered(
                credential.getCredentialId(),
                credential.getUserId(),
                credential.getCredentialType().name(),
                credential.getCredentialReference(),
                credential.getStatus().name(),
                credential.isMfaEnabled()
        ));
    }

    @Transactional
    public CompletableFuture<AccountEvent> signIn(SignInCommand command) {
        Optional<UserProfile> user = userProfileRepository.findByEmail(command.email());
        if (user.isEmpty()) {
            return publishFailedSignIn(null, command.email(), "User not found");
        }

        UserProfile profile = user.get();
        if (profile.getStatus() != UserStatus.ACTIVE) {
            return publishFailedSignIn(profile.getUserId(), command.email(), "User is not active");
        }

        Optional<UserCredential> credential = userCredentialRepository.findByUserIdAndCredentialReference(
                profile.getUserId(),
                command.credentialReference()
        );
        if (credential.isEmpty() || credential.get().getStatus() != CredentialStatus.ACTIVE) {
            return publishFailedSignIn(profile.getUserId(), command.email(), "Credential was not accepted");
        }

        SignInAttempt attempt = signInAttemptRepository.save(SignInAttempt.success(profile.getUserId(), command.email()));
        return eventPublisher.publish(AccountEvent.signInSucceeded(
                attempt.getSignInAttemptId(),
                profile.getUserId(),
                command.email()
        ));
    }

    @Transactional
    public CompletableFuture<AccountEvent> requestPasswordReset(RequestPasswordResetCommand command) {
        UserProfile user = userProfileRepository.findByEmail(command.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + command.email()));
        PasswordResetRequest resetRequest = passwordResetRequestRepository.save(PasswordResetRequest.request(
                user.getUserId(),
                user.getEmail()
        ));

        return eventPublisher.publish(AccountEvent.passwordResetRequested(
                resetRequest.getResetRequestId(),
                user.getUserId(),
                user.getEmail()
        ));
    }

    @Transactional
    public CompletableFuture<AccountEvent> completePasswordReset(CompletePasswordResetCommand command) {
        PasswordResetRequest resetRequest = passwordResetRequestRepository.findById(command.resetRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Password reset request not found: " + command.resetRequestId()));
        if (resetRequest.getStatus() != PasswordResetStatus.REQUESTED) {
            throw new IllegalArgumentException("Password reset request is already " + resetRequest.getStatus());
        }

        UserCredential credential = userCredentialRepository.save(UserCredential.register(
                resetRequest.getUserId(),
                CredentialType.PASSWORD_HASH,
                command.newCredentialReference(),
                command.mfaEnabled()
        ));
        resetRequest.complete();
        passwordResetRequestRepository.save(resetRequest);

        return eventPublisher.publish(AccountEvent.passwordResetCompleted(
                resetRequest.getResetRequestId(),
                resetRequest.getUserId(),
                resetRequest.getEmail(),
                credential.getCredentialId(),
                credential.isMfaEnabled()
        ));
    }

    private CompletableFuture<AccountEvent> publishFailedSignIn(UUID userId, String email, String reason) {
        SignInAttempt attempt = signInAttemptRepository.save(SignInAttempt.failure(userId, email, reason));
        if (attempt.getResult() != AuthenticationResult.FAILURE) {
            throw new IllegalStateException("Expected failed sign-in attempt");
        }
        return eventPublisher.publish(AccountEvent.signInFailed(
                attempt.getSignInAttemptId(),
                userId,
                email,
                reason
        ));
    }
}

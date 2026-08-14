package com.balaji.kafkaeventlab.account.api;

import com.balaji.kafkaeventlab.account.domain.PasswordResetRequest;
import com.balaji.kafkaeventlab.account.domain.PasswordResetRequestRepository;
import com.balaji.kafkaeventlab.account.domain.SignInAttempt;
import com.balaji.kafkaeventlab.account.domain.SignInAttemptRepository;
import com.balaji.kafkaeventlab.account.domain.UserCredential;
import com.balaji.kafkaeventlab.account.domain.UserCredentialRepository;
import com.balaji.kafkaeventlab.account.domain.UserProfile;
import com.balaji.kafkaeventlab.account.domain.UserProfileRepository;
import com.balaji.kafkaeventlab.account.tracking.AccountTrackedEvent;
import com.balaji.kafkaeventlab.account.tracking.AccountTrackedEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountQueryController {

    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final SignInAttemptRepository signInAttemptRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final AccountTrackedEventRepository trackedEventRepository;

    public AccountQueryController(
            UserProfileRepository userProfileRepository,
            UserCredentialRepository userCredentialRepository,
            SignInAttemptRepository signInAttemptRepository,
            PasswordResetRequestRepository passwordResetRequestRepository,
            AccountTrackedEventRepository trackedEventRepository
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.signInAttemptRepository = signInAttemptRepository;
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.trackedEventRepository = trackedEventRepository;
    }

    @GetMapping("/users")
    public List<UserProfile> users() {
        return userProfileRepository.findAll();
    }

    @GetMapping("/users/{userId}/credentials")
    public List<UserCredential> credentials(@PathVariable UUID userId) {
        return userCredentialRepository.findByUserId(userId);
    }

    @GetMapping("/users/{userId}/sign-in-attempts")
    public List<SignInAttempt> signInAttempts(@PathVariable UUID userId) {
        return signInAttemptRepository.findByUserId(userId);
    }

    @GetMapping("/users/{userId}/password-reset-requests")
    public List<PasswordResetRequest> passwordResetRequests(@PathVariable UUID userId) {
        return passwordResetRequestRepository.findByUserId(userId);
    }

    @GetMapping("/tracked-events")
    public List<AccountTrackedEvent> trackedEvents() {
        return trackedEventRepository.findAll();
    }

    @GetMapping("/users/{userId}/tracked-events")
    public List<AccountTrackedEvent> trackedEventsForUser(@PathVariable UUID userId) {
        return trackedEventRepository.findBySubjectUserId(userId);
    }
}

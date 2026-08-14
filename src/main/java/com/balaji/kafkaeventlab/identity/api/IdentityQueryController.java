package com.balaji.kafkaeventlab.identity.api;

import com.balaji.kafkaeventlab.identity.domain.ApplicationAccessGrant;
import com.balaji.kafkaeventlab.identity.domain.ApplicationAccessGrantRepository;
import com.balaji.kafkaeventlab.identity.domain.IdentityApplication;
import com.balaji.kafkaeventlab.identity.domain.IdentityApplicationRepository;
import com.balaji.kafkaeventlab.identity.domain.UserCredential;
import com.balaji.kafkaeventlab.identity.domain.UserCredentialRepository;
import com.balaji.kafkaeventlab.identity.domain.UserProfile;
import com.balaji.kafkaeventlab.identity.domain.UserProfileRepository;
import com.balaji.kafkaeventlab.identity.projection.UserAccessSummaryProjection;
import com.balaji.kafkaeventlab.identity.projection.UserAccessSummaryRepository;
import com.balaji.kafkaeventlab.identity.tracking.IdentityTrackedEvent;
import com.balaji.kafkaeventlab.identity.tracking.IdentityTrackedEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/identity")
public class IdentityQueryController {

    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final IdentityApplicationRepository applicationRepository;
    private final ApplicationAccessGrantRepository accessGrantRepository;
    private final UserAccessSummaryRepository accessSummaryRepository;
    private final IdentityTrackedEventRepository trackedEventRepository;

    public IdentityQueryController(
            UserProfileRepository userProfileRepository,
            UserCredentialRepository userCredentialRepository,
            IdentityApplicationRepository applicationRepository,
            ApplicationAccessGrantRepository accessGrantRepository,
            UserAccessSummaryRepository accessSummaryRepository,
            IdentityTrackedEventRepository trackedEventRepository
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.applicationRepository = applicationRepository;
        this.accessGrantRepository = accessGrantRepository;
        this.accessSummaryRepository = accessSummaryRepository;
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

    @GetMapping("/users/{userId}/access-grants")
    public List<ApplicationAccessGrant> accessGrants(@PathVariable UUID userId) {
        return accessGrantRepository.findByUserId(userId);
    }

    @GetMapping("/applications")
    public List<IdentityApplication> applications() {
        return applicationRepository.findAll();
    }

    @GetMapping("/access-summaries")
    public List<UserAccessSummaryProjection> accessSummaries() {
        return accessSummaryRepository.findAll();
    }

    @GetMapping("/tracked-events")
    public List<IdentityTrackedEvent> trackedEvents() {
        return trackedEventRepository.findAll();
    }

    @GetMapping("/users/{userId}/tracked-events")
    public List<IdentityTrackedEvent> trackedEventsForUser(@PathVariable UUID userId) {
        return trackedEventRepository.findBySubjectUserId(userId);
    }
}

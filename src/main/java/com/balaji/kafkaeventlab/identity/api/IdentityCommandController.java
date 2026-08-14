package com.balaji.kafkaeventlab.identity.api;

import com.balaji.kafkaeventlab.identity.application.CreateApplicationCommand;
import com.balaji.kafkaeventlab.identity.application.GrantApplicationAccessCommand;
import com.balaji.kafkaeventlab.identity.application.IdentityCommandService;
import com.balaji.kafkaeventlab.identity.application.RegisterCredentialCommand;
import com.balaji.kafkaeventlab.identity.application.RegisterUserCommand;
import com.balaji.kafkaeventlab.identity.application.RevokeApplicationAccessCommand;
import com.balaji.kafkaeventlab.identity.application.UpdateUserStatusCommand;
import com.balaji.kafkaeventlab.identity.event.IdentityEvent;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/identity")
public class IdentityCommandController {

    private final IdentityCommandService commandService;

    public IdentityCommandController(IdentityCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<IdentityEvent> registerUser(@Valid @RequestBody RegisterUserCommand command) {
        return commandService.registerUser(command);
    }

    @PostMapping("/users/{userId}/status")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<IdentityEvent> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusCommand command
    ) {
        return commandService.updateUserStatus(userId, command);
    }

    @PostMapping("/credentials")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<IdentityEvent> registerCredential(@Valid @RequestBody RegisterCredentialCommand command) {
        return commandService.registerCredential(command);
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<IdentityEvent> createApplication(@Valid @RequestBody CreateApplicationCommand command) {
        return commandService.createApplication(command);
    }

    @PostMapping("/access-grants")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<IdentityEvent> grantAccess(@Valid @RequestBody GrantApplicationAccessCommand command) {
        return commandService.grantAccess(command);
    }

    @PostMapping("/access-grants/revoke")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<IdentityEvent> revokeAccess(@Valid @RequestBody RevokeApplicationAccessCommand command) {
        return commandService.revokeAccess(command);
    }
}

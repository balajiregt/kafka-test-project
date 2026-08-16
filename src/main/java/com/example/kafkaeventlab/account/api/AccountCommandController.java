package com.example.kafkaeventlab.account.api;

import com.example.kafkaeventlab.account.application.CompletePasswordResetCommand;
import com.example.kafkaeventlab.account.application.AccountCommandService;
import com.example.kafkaeventlab.account.application.RegisterCredentialCommand;
import com.example.kafkaeventlab.account.application.RegisterUserCommand;
import com.example.kafkaeventlab.account.application.RequestPasswordResetCommand;
import com.example.kafkaeventlab.account.application.SignInCommand;
import com.example.kafkaeventlab.account.application.UpdateUserProfileCommand;
import com.example.kafkaeventlab.account.application.UpdateUserStatusCommand;
import com.example.kafkaeventlab.account.event.AccountEvent;
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
@RequestMapping("/api/accounts")
public class AccountCommandController {

    private final AccountCommandService commandService;

    public AccountCommandController(AccountCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<AccountEvent> registerUser(@Valid @RequestBody RegisterUserCommand command) {
        return commandService.registerUser(command);
    }

    @PostMapping("/users/{userId}/status")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<AccountEvent> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusCommand command
    ) {
        return commandService.updateUserStatus(userId, command);
    }

    @PostMapping("/users/{userId}/profile")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<AccountEvent> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserProfileCommand command
    ) {
        return commandService.updateUserProfile(userId, command);
    }

    @PostMapping("/credentials")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<AccountEvent> registerCredential(@Valid @RequestBody RegisterCredentialCommand command) {
        return commandService.registerCredential(command);
    }

    @PostMapping("/sign-ins")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<AccountEvent> signIn(@Valid @RequestBody SignInCommand command) {
        return commandService.signIn(command);
    }

    @PostMapping("/password-reset-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<AccountEvent> requestPasswordReset(@Valid @RequestBody RequestPasswordResetCommand command) {
        return commandService.requestPasswordReset(command);
    }

    @PostMapping("/password-resets/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<AccountEvent> completePasswordReset(@Valid @RequestBody CompletePasswordResetCommand command) {
        return commandService.completePasswordReset(command);
    }

}

package com.valadir.web.adapter;

import com.valadir.application.command.CompleteEmailChangeCommand;
import com.valadir.application.command.InitiateEmailChangeCommand;
import com.valadir.application.port.in.CompleteEmailChangeUseCase;
import com.valadir.application.port.in.InitiateEmailChangeUseCase;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.dto.request.CompleteEmailChangeRequest;
import com.valadir.web.dto.request.InitiateEmailChangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.Auth.BASE)
class EmailChangeController {

    private final InitiateEmailChangeUseCase initiateEmailChangeUseCase;
    private final CompleteEmailChangeUseCase completeEmailChangeUseCase;

    EmailChangeController(InitiateEmailChangeUseCase initiateEmailChangeUseCase, CompleteEmailChangeUseCase completeEmailChangeUseCase) {

        this.initiateEmailChangeUseCase = initiateEmailChangeUseCase;
        this.completeEmailChangeUseCase = completeEmailChangeUseCase;
    }

    @PostMapping(ApiRoutes.Auth.Account.INITIATE_EMAIL_CHANGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void initiateEmailChange(@Valid @RequestBody InitiateEmailChangeRequest request, @AuthenticationPrincipal Jwt jwt) {

        var command = new InitiateEmailChangeCommand(jwt.getSubject(), request.newEmail(), request.password());

        initiateEmailChangeUseCase.initiate(command);
    }

    @PostMapping(ApiRoutes.Auth.Account.COMPLETE_EMAIL_CHANGE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void completeEmailChange(@Valid @RequestBody CompleteEmailChangeRequest request, @AuthenticationPrincipal Jwt jwt) {

        var command = new CompleteEmailChangeCommand(jwt.getSubject(), request.code());

        completeEmailChangeUseCase.complete(command);
    }
}

package com.valadir.web.adapter;

import com.valadir.application.command.ActivateAccountCommand;
import com.valadir.application.command.RegisterCommand;
import com.valadir.application.command.ResendAccountActivationCodeCommand;
import com.valadir.application.port.in.ActivateAccountUseCase;
import com.valadir.application.port.in.RegisterUseCase;
import com.valadir.application.port.in.ResendAccountActivationCodeUseCase;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.dto.request.ActivateAccountRequest;
import com.valadir.web.dto.request.RegisterRequest;
import com.valadir.web.dto.request.ResendAccountActivationCodeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.Auth.BASE)
class AccountRegistrationController {

    private final RegisterUseCase registerUseCase;
    private final ActivateAccountUseCase activateAccountUseCase;
    private final ResendAccountActivationCodeUseCase resendAccountActivationCodeUseCase;

    AccountRegistrationController(
        RegisterUseCase registerUseCase,
        ActivateAccountUseCase activateAccountUseCase,
        ResendAccountActivationCodeUseCase resendAccountActivationCodeUseCase
    ) {

        this.registerUseCase = registerUseCase;
        this.activateAccountUseCase = activateAccountUseCase;
        this.resendAccountActivationCodeUseCase = resendAccountActivationCodeUseCase;
    }

    @PostMapping(ApiRoutes.Auth.Registration.REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    void register(@Valid @RequestBody RegisterRequest request) {

        registerUseCase.register(new RegisterCommand(
            request.email(),
            request.password(),
            request.fullName(),
            request.givenName()
        ));
    }

    @PostMapping(ApiRoutes.Auth.Registration.ACTIVATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void activateAccount(@Valid @RequestBody ActivateAccountRequest request) {

        var command = new ActivateAccountCommand(request.email(), request.code());

        activateAccountUseCase.activate(command);
    }

    @PostMapping(ApiRoutes.Auth.Registration.RESEND)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resendAccountActivationCode(@Valid @RequestBody ResendAccountActivationCodeRequest request) {

        var command = new ResendAccountActivationCodeCommand(request.email());

        resendAccountActivationCodeUseCase.resend(command);
    }
}

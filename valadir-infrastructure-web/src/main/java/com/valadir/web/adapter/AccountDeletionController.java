package com.valadir.web.adapter;

import com.valadir.application.command.DeleteAccountCommand;
import com.valadir.application.port.in.DeleteAccountUseCase;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.dto.request.DeleteAccountRequest;
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
class AccountDeletionController {

    private final DeleteAccountUseCase deleteAccountUseCase;

    AccountDeletionController(DeleteAccountUseCase deleteAccountUseCase) {

        this.deleteAccountUseCase = deleteAccountUseCase;
    }

    @PostMapping(ApiRoutes.Auth.Account.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAccount(@Valid @RequestBody DeleteAccountRequest request, @AuthenticationPrincipal Jwt jwt) {

        deleteAccountUseCase.delete(new DeleteAccountCommand(jwt.getSubject(), request.password()));
    }
}

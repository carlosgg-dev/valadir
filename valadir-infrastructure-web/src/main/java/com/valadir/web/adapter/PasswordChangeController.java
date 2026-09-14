package com.valadir.web.adapter;

import com.valadir.application.command.ChangePasswordCommand;
import com.valadir.application.port.in.ChangePasswordUseCase;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.dto.request.ChangePasswordRequest;
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
class PasswordChangeController {

    private final ChangePasswordUseCase changePasswordUseCase;

    PasswordChangeController(ChangePasswordUseCase changePasswordUseCase) {

        this.changePasswordUseCase = changePasswordUseCase;
    }

    @PostMapping(ApiRoutes.Auth.Account.CHANGE_PASSWORD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@Valid @RequestBody ChangePasswordRequest request, @AuthenticationPrincipal Jwt jwt) {

        var command = new ChangePasswordCommand(jwt.getSubject(), request.currentPassword(), request.newPassword());

        changePasswordUseCase.change(command);
    }
}

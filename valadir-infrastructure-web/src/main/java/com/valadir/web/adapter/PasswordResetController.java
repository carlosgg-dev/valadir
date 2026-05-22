package com.valadir.web.adapter;

import com.valadir.application.command.CompletePasswordResetCommand;
import com.valadir.application.command.InitiatePasswordResetCommand;
import com.valadir.application.command.VerifyPasswordResetOtpCommand;
import com.valadir.application.port.in.CompletePasswordResetUseCase;
import com.valadir.application.port.in.InitiatePasswordResetUseCase;
import com.valadir.application.port.in.VerifyPasswordResetOtpUseCase;
import com.valadir.application.result.PasswordResetOtpVerificationResult;
import com.valadir.domain.model.PlainOtp;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.dto.request.CompletePasswordResetRequest;
import com.valadir.web.dto.request.InitiatePasswordResetRequest;
import com.valadir.web.dto.request.VerifyPasswordResetOtpRequest;
import com.valadir.web.dto.response.PasswordResetOtpVerificationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.Auth.BASE)
class PasswordResetController {

    private final InitiatePasswordResetUseCase initiatePasswordResetUseCase;
    private final VerifyPasswordResetOtpUseCase verifyPasswordResetOtpUseCase;
    private final CompletePasswordResetUseCase completePasswordResetUseCase;

    PasswordResetController(
        InitiatePasswordResetUseCase initiatePasswordResetUseCase,
        VerifyPasswordResetOtpUseCase verifyPasswordResetOtpUseCase,
        CompletePasswordResetUseCase completePasswordResetUseCase
    ) {

        this.initiatePasswordResetUseCase = initiatePasswordResetUseCase;
        this.verifyPasswordResetOtpUseCase = verifyPasswordResetOtpUseCase;
        this.completePasswordResetUseCase = completePasswordResetUseCase;
    }

    @PostMapping(ApiRoutes.Auth.PasswordReset.INITIATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void initiatePasswordReset(@Valid @RequestBody InitiatePasswordResetRequest request) {

        initiatePasswordResetUseCase.initiate(new InitiatePasswordResetCommand(request.email()));
    }

    @PostMapping(ApiRoutes.Auth.PasswordReset.VERIFY)
    PasswordResetOtpVerificationResponse verifyPasswordResetOtp(@Valid @RequestBody VerifyPasswordResetOtpRequest request) {

        PasswordResetOtpVerificationResult result = verifyPasswordResetOtpUseCase.verify(new VerifyPasswordResetOtpCommand(request.email(), PlainOtp.from(request.code())));

        return new PasswordResetOtpVerificationResponse(result.verificationToken());
    }

    @PostMapping(ApiRoutes.Auth.PasswordReset.COMPLETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void completePasswordReset(@Valid @RequestBody CompletePasswordResetRequest request) {

        completePasswordResetUseCase.complete(new CompletePasswordResetCommand(request.verificationToken(), request.newPassword()));
    }
}

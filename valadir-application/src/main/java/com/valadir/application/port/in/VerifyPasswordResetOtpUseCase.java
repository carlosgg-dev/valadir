package com.valadir.application.port.in;

import com.valadir.application.command.VerifyPasswordResetOtpCommand;
import com.valadir.application.result.PasswordResetOtpVerificationResult;

public interface VerifyPasswordResetOtpUseCase {

    PasswordResetOtpVerificationResult verify(VerifyPasswordResetOtpCommand command);
}

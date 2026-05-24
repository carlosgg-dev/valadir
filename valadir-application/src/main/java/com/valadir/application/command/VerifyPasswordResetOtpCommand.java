package com.valadir.application.command;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;

public record VerifyPasswordResetOtpCommand(
    Email email,
    PlainOtp plainOtp) {

}

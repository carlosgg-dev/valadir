package com.valadir.application.command;

import com.valadir.domain.model.PlainOtp;

public record VerifyPasswordResetOtpCommand(
    String email,
    PlainOtp plainOtp) {

}

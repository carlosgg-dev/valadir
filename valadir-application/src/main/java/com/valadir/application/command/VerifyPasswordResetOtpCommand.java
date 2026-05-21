package com.valadir.application.command;

import com.valadir.application.otp.PlainOtp;

public record VerifyPasswordResetOtpCommand(
    String email,
    PlainOtp plainOtp) {

}

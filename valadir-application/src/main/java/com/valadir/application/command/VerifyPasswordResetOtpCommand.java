package com.valadir.application.command;

public record VerifyPasswordResetOtpCommand(
    String email,
    String code) {

}

package com.valadir.application.command;

public record CompletePasswordResetCommand(
    String verificationToken,
    String newPassword) {

}

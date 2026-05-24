package com.valadir.application.command;

import com.valadir.domain.model.RawPassword;

public record CompletePasswordResetCommand(
    String verificationToken,
    RawPassword newPassword) {

}

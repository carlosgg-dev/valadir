package com.valadir.application.command;

import com.valadir.application.otp.PlainOtp;

public record ActivateAccountCommand(
    String email,
    PlainOtp plainOtp) {

}

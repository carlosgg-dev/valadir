package com.valadir.application.command;

import com.valadir.domain.model.PlainOtp;

public record ActivateAccountCommand(
    String email,
    PlainOtp plainOtp) {

}

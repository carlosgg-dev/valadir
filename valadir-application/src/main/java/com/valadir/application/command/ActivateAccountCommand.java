package com.valadir.application.command;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.PlainOtp;

public record ActivateAccountCommand(
    Email email,
    PlainOtp plainOtp) {

}

package com.valadir.application.command;

import com.valadir.domain.model.Email;

public record InitiatePasswordResetCommand(
    Email email) {

}

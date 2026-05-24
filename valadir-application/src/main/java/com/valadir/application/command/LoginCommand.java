package com.valadir.application.command;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.RawPassword;

public record LoginCommand(
    Email email,
    RawPassword password) {

}

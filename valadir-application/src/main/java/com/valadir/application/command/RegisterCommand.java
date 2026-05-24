package com.valadir.application.command;

import com.valadir.domain.model.Email;
import com.valadir.domain.model.FullName;
import com.valadir.domain.model.GivenName;
import com.valadir.domain.model.RawPassword;

public record RegisterCommand(
    Email email,
    RawPassword password,
    FullName fullName,
    GivenName givenName) {

}

package com.valadir.application.port.in;

import com.valadir.application.command.LoginCommand;
import com.valadir.application.result.AuthTokenResult;

public interface LoginUseCase {

    AuthTokenResult login(LoginCommand command);
}

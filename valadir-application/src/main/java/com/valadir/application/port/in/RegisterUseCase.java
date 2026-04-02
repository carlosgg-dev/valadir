package com.valadir.application.port.in;

import com.valadir.application.command.RegisterCommand;
import com.valadir.application.result.AuthTokenResult;

public interface RegisterUseCase {

    AuthTokenResult register(RegisterCommand command);
}

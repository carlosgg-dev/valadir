package com.valadir.application.port.in;

import com.valadir.application.command.LogoutCommand;

public interface LogoutUseCase {

    void logout(LogoutCommand command);
}

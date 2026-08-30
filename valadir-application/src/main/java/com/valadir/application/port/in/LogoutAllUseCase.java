package com.valadir.application.port.in;

import com.valadir.application.command.LogoutAllCommand;

public interface LogoutAllUseCase {

    void logoutAll(LogoutAllCommand command);
}

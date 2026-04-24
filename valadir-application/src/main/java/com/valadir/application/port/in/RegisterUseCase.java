package com.valadir.application.port.in;

import com.valadir.application.command.RegisterCommand;

public interface RegisterUseCase {

    void register(RegisterCommand command);
}

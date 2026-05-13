package com.valadir.application.port.in;

import com.valadir.application.command.ActivateAccountCommand;

public interface ActivateAccountUseCase {

    void activate(ActivateAccountCommand command);
}

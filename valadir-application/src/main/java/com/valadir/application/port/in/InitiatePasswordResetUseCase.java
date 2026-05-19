package com.valadir.application.port.in;

import com.valadir.application.command.InitiatePasswordResetCommand;

public interface InitiatePasswordResetUseCase {

    void initiate(InitiatePasswordResetCommand command);
}

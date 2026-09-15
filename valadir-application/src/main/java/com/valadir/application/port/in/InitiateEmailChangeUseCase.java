package com.valadir.application.port.in;

import com.valadir.application.command.InitiateEmailChangeCommand;

public interface InitiateEmailChangeUseCase {

    void initiate(InitiateEmailChangeCommand command);
}

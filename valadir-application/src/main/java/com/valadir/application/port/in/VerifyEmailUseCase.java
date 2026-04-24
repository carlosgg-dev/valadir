package com.valadir.application.port.in;

import com.valadir.application.command.VerifyEmailCommand;

public interface VerifyEmailUseCase {

    void verify(VerifyEmailCommand command);
}

package com.valadir.application.port.in;

import com.valadir.application.command.ResendVerificationCommand;

public interface ResendVerificationUseCase {

    void resend(ResendVerificationCommand command);
}

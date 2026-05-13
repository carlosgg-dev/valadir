package com.valadir.application.port.in;

import com.valadir.application.command.ResendAccountActivationCodeCommand;

public interface ResendAccountActivationCodeUseCase {

    void resend(ResendAccountActivationCodeCommand command);
}

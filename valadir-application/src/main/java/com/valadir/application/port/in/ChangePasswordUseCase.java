package com.valadir.application.port.in;

import com.valadir.application.command.ChangePasswordCommand;

public interface ChangePasswordUseCase {

    void change(ChangePasswordCommand command);
}

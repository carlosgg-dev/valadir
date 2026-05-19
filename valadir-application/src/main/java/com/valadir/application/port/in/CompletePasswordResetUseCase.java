package com.valadir.application.port.in;

import com.valadir.application.command.CompletePasswordResetCommand;

public interface CompletePasswordResetUseCase {

    void complete(CompletePasswordResetCommand command);
}

package com.valadir.application.port.in;

import com.valadir.application.command.CompleteEmailChangeCommand;

public interface CompleteEmailChangeUseCase {

    void complete(CompleteEmailChangeCommand command);
}

package com.valadir.application.port.in;

import com.valadir.application.command.DeleteAccountCommand;

public interface DeleteAccountUseCase {

    void delete(DeleteAccountCommand command);
}

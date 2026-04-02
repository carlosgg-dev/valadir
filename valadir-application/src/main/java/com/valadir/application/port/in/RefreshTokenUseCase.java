package com.valadir.application.port.in;

import com.valadir.application.command.RefreshTokenCommand;
import com.valadir.application.result.AuthTokenResult;

public interface RefreshTokenUseCase {

    AuthTokenResult refresh(RefreshTokenCommand command);
}

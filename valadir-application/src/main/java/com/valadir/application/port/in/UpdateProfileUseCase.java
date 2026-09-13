package com.valadir.application.port.in;

import com.valadir.application.command.UpdateProfileCommand;
import com.valadir.application.result.ProfileResult;

public interface UpdateProfileUseCase {

    ProfileResult update(UpdateProfileCommand command);
}

package com.valadir.application.port.in;

import com.valadir.application.result.ProfileResult;

public interface GetProfileUseCase {

    ProfileResult getProfile(String accountId);
}

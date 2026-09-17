package com.valadir.application.port.out;

import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;

public interface PasswordHasher {

    HashedPassword hash(RawPassword password);

    boolean matches(RawPassword rawPassword, HashedPassword hashedPassword);
}

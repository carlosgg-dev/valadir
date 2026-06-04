package com.valadir.test.mother;

import com.valadir.domain.model.HashedPassword;
import com.valadir.domain.model.RawPassword;

public final class PasswordMother {

    private PasswordMother() {

    }

    public static RawPassword raw() {

        return RawPassword.from("SecureP@ss123");
    }

    public static HashedPassword hashed() {

        return new HashedPassword("$argon2id$v=19$m=65536,t=3,p=4$testPassword");
    }
}

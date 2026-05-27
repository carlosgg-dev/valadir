package com.valadir.test.mother;

import com.valadir.domain.model.HashedOtp;
import com.valadir.domain.model.PlainOtp;

public final class OtpMother {

    private OtpMother() {

    }

    public static PlainOtp plain() {

        return PlainOtp.generate();
    }

    public static HashedOtp hashed() {

        return new HashedOtp("$argon2id$v=19$m=65536,t=3,p=4$testOtp");
    }
}
